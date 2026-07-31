package vocabulary.app.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vocabulary.app.entity.User;
import vocabulary.app.exception.LlmUnavailableException;
import vocabulary.app.exception.RateLimitExceededException;
import vocabulary.app.jwt.JwtAuthenticationFilter;
import vocabulary.app.security.OwnershipGuard;
import vocabulary.app.service.OpenAIService;
import vocabulary.app.service.WordMeaningService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// /create_word_meaning 의 HTTP 계약.
//
// 확장은 "뜻이 없음"과 "서버가 죽음"에 다르게 반응해야 하므로 상태 코드가 갈리는지가 핵심이다.
// 인증 자체는 SecurityConfig(anyRequest().authenticated())가 담당하므로 여기서는 필터를 끄고 본문·코드만 본다.
@WebMvcTest(controllers = WebController.class)
@AutoConfigureMockMvc(addFilters = false)
class WebControllerTest {

    private static final String REQUEST_BODY = """
            {
              "base_form": "生物",
              "word": "生物",
              "reading": "ナマモノ",
              "pos": "名詞",
              "pos_detail": "一般",
              "target_language": "ko"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenAIService openAIService;

    @MockitoBean
    private WordMeaningService wordMeaningService;

    @MockitoBean
    private OwnershipGuard ownershipGuard;

    // JwtAuthenticationFilter는 Filter라서 @WebMvcTest가 스캔 대상에 넣는다.
    // 실물을 만들면 JwtTokenProvider까지 끌려오므로 대체한다. (addFilters=false라 실행되지도 않는다)
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        when(ownershipGuard.currentUser(any())).thenReturn(user);
    }

    @Test
    @DisplayName("뜻을 찾으면 200 + meaning")
    void returnsMeaning() throws Exception {
        when(wordMeaningService.resolveMeaning(any(), any())).thenReturn("날것, 신선식품");

        mockMvc.perform(post("/create_word_meaning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meaning").value("날것, 신선식품"));
    }

    // 확장은 이 응답을 보고 칸을 비워두고 사용자 입력을 기다린다. 필드 자체가 빠지면 안 된다.
    @Test
    @DisplayName("뜻을 판정하지 못하면 200 + meaning:null")
    void returnsNullMeaningWithOk() throws Exception {
        when(wordMeaningService.resolveMeaning(any(), any())).thenReturn(null);

        mockMvc.perform(post("/create_word_meaning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"meaning\":null}"));
    }

    @Test
    @DisplayName("LLM 장애는 200 이 아니라 503")
    void llmFailureIsNotOk() throws Exception {
        when(wordMeaningService.resolveMeaning(any(), any()))
                .thenThrow(new LlmUnavailableException("boom"));

        mockMvc.perform(post("/create_word_meaning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("LLM_UNAVAILABLE"));
    }

    @Test
    @DisplayName("호출 제한 초과는 429 + Retry-After")
    void rateLimitIsTooManyRequests() throws Exception {
        when(wordMeaningService.resolveMeaning(any(), any()))
                .thenThrow(new RateLimitExceededException("너무 잦습니다", 42));

        mockMvc.perform(post("/create_word_meaning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"));
    }

    @Test
    @DisplayName("필수 필드가 빠지면 400")
    void missingFieldIsBadRequest() throws Exception {
        when(wordMeaningService.resolveMeaning(any(), any()))
                .thenThrow(new IllegalArgumentException("reading 은(는) 필수입니다."));

        mockMvc.perform(post("/create_word_meaning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"base_form\":\"生物\",\"word\":\"生物\",\"target_language\":\"ko\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
