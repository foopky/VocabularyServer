package vocabulary.app.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// /create_word_meaning 의 LLM 조회 결과를 담아두는 캐시.
//
// 사용자별이 아니라 "사용자 전체를 가로지르는" 캐시다. 학습자들이 담는 단어는 겹치는 폭이 넓어서,
// 食べる 를 누가 한 번 조회하면 그 뒤의 모든 한국어 사용자가 같은 결과를 쓴다.
// 요청에 문장 문맥이 없으므로 같은 입력은 항상 같은 답이 되고, 캐시가 갈릴 일이 없다.
//
// 메모리가 아니라 DB에 두는 이유: 배포할 때마다 날아가면 "전체 사용자가 공유한다"는 이점이 사라진다.
@Entity
@Table(
        name = "word_meaning_cache",
        // 캐시 키. pos_detail 은 키에 넣지 않는다 (pos 를 이미 아는 상태에서 뜻을 가르는 일이 거의 없다).
        uniqueConstraints = @UniqueConstraint(
                name = "uk_word_meaning_cache_key",
                columnNames = {"base_form", "reading", "pos", "target_language"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordMeaningCache {

    @Schema(hidden = true)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_meaning_cache_id")
    private Long id;

    // 사전형. 자막에 뜬 활용형(word)이 아니라 이 값이 조회의 기준이다.
    @Column(name = "base_form", nullable = false)
    private String baseForm;

    // 가타카나 읽기. 표기가 같아도 읽기가 다르면 다른 단어이므로 (生物 = セイブツ / ナマモノ)
    // 반드시 키에 포함되어야 한다. 빠지면 캐시가 조용히 틀린 뜻을 돌려준다.
    @Column(nullable = false)
    private String reading;

    // 품사는 선택 입력이라 비어 올 수 있다.
    // PostgreSQL 의 UNIQUE 는 NULL 끼리를 서로 다른 값으로 보므로, 빈 문자열로 정규화해 저장한다.
    @Column(nullable = false)
    private String pos;

    @Column(name = "target_language", nullable = false)
    private String targetLanguage;

    @Column(nullable = false)
    private String meaning;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private WordMeaningCache(String baseForm, String reading, String pos, String targetLanguage, String meaning) {
        this.baseForm = baseForm;
        this.reading = reading;
        this.pos = pos;
        this.targetLanguage = targetLanguage;
        this.meaning = meaning;
        this.createdAt = Instant.now();
    }

    public static WordMeaningCache of(String baseForm, String reading, String pos, String targetLanguage, String meaning) {
        return new WordMeaningCache(baseForm, reading, pos, targetLanguage, meaning);
    }
}
