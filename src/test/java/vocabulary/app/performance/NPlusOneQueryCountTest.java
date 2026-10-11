package vocabulary.app.performance;

import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.SoftAssertions;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vocabulary.app.dto.SharedFolderDTO;
import vocabulary.app.entity.Role;
import vocabulary.app.entity.Sentence;
import vocabulary.app.entity.SharedFolder;
import vocabulary.app.entity.User;
import vocabulary.app.entity.Word;
import vocabulary.app.entity.WordFolder;
import vocabulary.app.jwt.JwtTokenProvider;
import vocabulary.app.repository.SentenceRepository;
import vocabulary.app.repository.SharedFolderRepository;
import vocabulary.app.repository.UserRepository;
import vocabulary.app.repository.WordFolderRepository;
import vocabulary.app.repository.WordRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 목록 조회 API의 N+1 문제를 "요청 1회당 실행된 SQL 개수"로 측정한다.
 *
 * <p>데이터 양을 늘려가며(N = 10 → 50 → 100) 같은 API를 호출한다.
 * N+1이 있으면 데이터에 비례해 쿼리가 늘고, 없으면 데이터 양과 무관하게 일정하다.
 *
 * <ul>
 *   <li>공유 폴더: 사용자 N명, 폴더 N개, 공유 글 N개를 1:1:1로 만든다.
 *       (공유 글 1개 = 작성자 1명 + 폴더 1개, 폴더 주인은 작성자 본인)</li>
 *   <li>예문: 측정용 사용자(me) 1명이 서로 다른 단어 N개에 예문을 1개씩 단다.
 *       (예문 1개 = 사용자 1명 + 단어 1개)</li>
 * </ul>
 *
 * <p>실제 요청 경로(Spring Security JWT 필터 → 컨트롤러 → 서비스 → JSON 직렬화)를 그대로 지나도록
 * MockMvc로 호출하고, JWT도 실제 {@link JwtTokenProvider}로 발급한다.
 *
 * <p>결과는 콘솔과 build/reports/nplusone/ 아래에 표로 남는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("nplusone")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NPlusOneQueryCountTest {

    /** 데이터 규모 N = 공유 글 수 = 예문 수. */
    static final int[] SCALES = {10, 50, 100};
    static final String ME = "nplusone-me";

    static final String SHARED_FOLDERS = "GET /sharedFolder";
    static final String SENTENCES_BY_USER = "GET /sentence/user/{userId}";

    @Autowired MockMvc mockMvc;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired WordFolderRepository wordFolderRepository;
    @Autowired SharedFolderRepository sharedFolderRepository;
    @Autowired WordRepository wordRepository;
    @Autowired SentenceRepository sentenceRepository;

    private Statistics statistics;
    private String bearerToken;
    private User me;
    private int seededOwners = 0;
    private int seededSentences = 0;

    /** API → (데이터 규모 → 요청 1회당 SQL 개수) */
    private final Map<String, Map<Integer, Long>> results = new LinkedHashMap<>();

    /** 리포트 파일 이름과 제목에 쓰는 라벨 */
    protected String label() {
        return "default";
    }

    @BeforeAll
    void measureAllScales() throws Exception {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        me = userRepository.save(newUser(ME));
        bearerToken = "Bearer " + jwtTokenProvider.createToken(ME);

        for (int scale : SCALES) {
            seedUpTo(scale);

            saveResult(SHARED_FOLDERS, scale,
                    countQueries(get("/sharedFolder"), scale));
            saveResult(SENTENCES_BY_USER, scale,
                    countQueries(get("/sentence/user/{userId}", me.getId()), scale));
        }
    }

    @Test
    @Order(1)
    void printReport() throws IOException {
        StringBuilder table = new StringBuilder();
        table.append("## N+1 query count (").append(label()).append(")\n\n");
        table.append("| API |");
        for (int scale : SCALES) {
            table.append(" N=").append(scale).append(" |");
        }
        table.append("\n|---|");
        for (int ignored : SCALES) {
            table.append("---:|");
        }
        table.append('\n');
        results.forEach((api, byScale) -> {
            table.append("| ").append(api).append(" |");
            for (int scale : SCALES) {
                table.append(' ').append(byScale.get(scale)).append(" |");
            }
            table.append('\n');
        });
        table.append("\n(값 = 요청 1회당 실행된 SQL 개수. 응답 건수 = N)\n");

        System.out.println("\n" + table);

        Path report = Path.of("build", "reports", "nplusone", label() + ".md");
        Files.createDirectories(report.getParent());
        Files.writeString(report, table.toString(), StandardCharsets.UTF_8);
    }

    /**
     * N+1이 없다면 데이터가 10배로 늘어도 쿼리 수는 같아야 한다.
     * 수정 전에는 실패하고, 수정 후에는 통과하는 회귀 테스트다.
     */
    @Test
    @Order(2)
    void queryCountDoesNotGrowWithData() {
        int smallest = SCALES[0];
        int largest = SCALES[SCALES.length - 1];

        SoftAssertions softly = new SoftAssertions();
        results.forEach((api, byScale) -> softly.assertThat(byScale.get(largest))
                .as("%s: N=%d 일 때와 N=%d 일 때 쿼리 수가 같아야 한다", api, smallest, largest)
                .isEqualTo(byScale.get(smallest)));
        softly.assertAll();
    }

    // ---------------------------------------------------------------- 측정

    private long countQueries(MockHttpServletRequestBuilder request, int expectedSize) throws Exception {
        statistics.clear();
        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                // 응답이 비어 있으면 쿼리 수 비교가 의미가 없으므로 건수까지 확인한다.
                .andExpect(jsonPath("$.length()").value(expectedSize));
        return statistics.getPrepareStatementCount();
    }

    private void saveResult(String api, int scale, long queryCount) {
        results.computeIfAbsent(api, k -> new LinkedHashMap<>()).put(scale, queryCount);
    }

    // ---------------------------------------------------------------- 데이터

    /** 이전 규모에서 만든 데이터는 그대로 두고, 부족한 만큼만 추가한다. */
    private void seedUpTo(int scale) {
        List<User> owners = new ArrayList<>();
        for (int i = seededOwners; i < scale; i++) {
            owners.add(newUser("owner-" + i));
        }
        // 공유 글 1개 = 사용자 1명 + 폴더 1개 (N:N:N)
        for (User owner : userRepository.saveAll(owners)) {
            WordFolder folder = new WordFolder();
            folder.setName(owner.getName() + "-folder");
            folder.setLanguage("english");
            folder.setUser(owner);
            folder = wordFolderRepository.save(folder);

            SharedFolderDTO dto = new SharedFolderDTO(owner.getId(), folder.getId());
            sharedFolderRepository.save(SharedFolder.create(dto, owner, folder));
        }
        seededOwners = scale;

        // 예문 1개 = 사용자(me) 1명 + 단어 1개, 단어는 예문마다 서로 다르다.
        for (int i = seededSentences; i < scale; i++) {
            Word word = wordRepository.save(Word.builder()
                    .language("english")
                    .pos("noun")
                    .word("word-" + i)
                    .meaning("meaning-" + i)
                    .learned(false)
                    .build());
            sentenceRepository.save(Sentence.builder()
                    .word(word)
                    .user(me)
                    .sentence("example sentence " + i)
                    .style("casual")
                    .meaning("example meaning " + i)
                    .build());
        }
        seededSentences = scale;
    }

    private static User newUser(String name) {
        User user = new User();
        user.setName(name);
        user.setPassword("not-used");
        user.setRole(Role.USER);
        return user;
    }
}
