package vocabulary.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vocabulary.app.dto.WordMeaningRequestDTO;
import vocabulary.app.dto.WordMeaningResponseDTO;
import vocabulary.app.security.OwnershipGuard;
import vocabulary.app.service.OpenAIService;
import vocabulary.app.service.WordMeaningService;

import java.util.List;

@RestController
@RequestMapping("/")
public class WebController {
    private final OpenAIService openAIService;
    private final WordMeaningService wordMeaningService;
    private final OwnershipGuard ownershipGuard;

    public WebController(OpenAIService openAIService,
                         WordMeaningService wordMeaningService,
                         OwnershipGuard ownershipGuard) {
        this.openAIService = openAIService;
        this.wordMeaningService = wordMeaningService;
        this.ownershipGuard = ownershipGuard;
    }

    @GetMapping("/create_sentences")
    public ResponseEntity<List<OpenAIService.ExampleResult>> createSentences(
            @RequestParam("word") String word,
            @RequestParam("language") String language,
            @RequestParam("style") String style) {
        List<OpenAIService.ExampleResult> sentences = openAIService.generateExamples(word, language, style);
        return ResponseEntity.ok(sentences);
    }

    // 확장의 "단어 추가" 모달에서 meaning 칸을 자동으로 채우기 위한 조회.
    //
    // 부가 기능이다. 이 엔드포인트가 느리거나 죽어도 단어 저장은 그대로 되고,
    // 확장은 응답을 기다리는 동안 모달을 막지 않는다.
    //
    // 상태 코드를 구분해서 내려주는 게 중요하다:
    //   200 + {"meaning": "..."}   뜻을 찾음
    //   200 + {"meaning": null}    뜻을 판정하지 못함        -> 확장은 칸을 비워둔다
    //   400                        요청 필드 누락
    //   401                        토큰 만료·무효           -> 확장이 refresh 후 1회 재시도한다
    //   429                        호출 제한 ("+" 연타)
    //   503                        OpenAI 장애·타임아웃      -> 확장은 서버 문제로 취급한다
    @Operation(summary = "단어 뜻 자동 입력",
            description = "일본어 단어의 사전형·읽기·품사를 받아 지정한 언어의 뜻 한 줄을 돌려준다. "
                    + "읽기가 다르면 다른 단어로 취급한다 (生物 セイブツ / ナマモノ).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "뜻을 찾았거나(meaning), 판정하지 못함(null)"),
            @ApiResponse(responseCode = "400", description = "base_form / reading / target_language 누락"),
            @ApiResponse(responseCode = "429", description = "사용자별 호출 제한 초과"),
            @ApiResponse(responseCode = "503", description = "LLM 호출 실패")
    })
    @PostMapping("/create_word_meaning")
    public ResponseEntity<WordMeaningResponseDTO> createWordMeaning(@RequestBody WordMeaningRequestDTO request,
                                                                    Authentication authentication) {
        // LLM 비용이 드는 경로라 인증이 필수다. (SecurityConfig 의 anyRequest().authenticated() 로 이미 막혀 있고,
        // 여기서는 호출 제한을 걸 사용자를 확정하기 위해 조회한다. 토큰은 유효한데 계정이 지워진 경우도 여기서 걸린다.)
        Long userId = ownershipGuard.currentUser(authentication).getId();
        return ResponseEntity.ok(WordMeaningResponseDTO.of(wordMeaningService.resolveMeaning(userId, request)));
    }
}
