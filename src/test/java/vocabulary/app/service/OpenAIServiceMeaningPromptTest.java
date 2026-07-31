package vocabulary.app.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 뜻 조회 프롬프트가 실제로 읽기를 반영하는지 확인한다.
//
// 확인의 기준점: 生物 를 ナマモノ / セイブツ 로 보냈을 때 서로 다른 뜻이 나와야 한다.
// 안 갈리면 십중팔구 reading 이 프롬프트에 안 들어간 것이므로, 여기서 먼저 막는다.
class OpenAIServiceMeaningPromptTest {

    // 네트워크를 타지 않는다. 프롬프트 조립만 확인한다.
    private final OpenAIService openAIService = new OpenAIService("test-key", "test-model", "low", 3000);

    @Test
    @DisplayName("프롬프트에 읽기와 동형이의어 지시가 들어간다")
    void promptCarriesReading() {
        String prompt = openAIService.buildMeaningPrompt("生物", "生物", "ナマモノ", "名詞", "一般", "Korean");

        assertThat(prompt).contains("ナマモノ");
        assertThat(prompt).contains("生物");
        assertThat(prompt).contains("名詞 / 一般");
        assertThat(prompt).contains("Korean");
        // 더 흔한 읽기로 끌려가지 말라는 지시
        assertThat(prompt).contains("matches the reading above");
    }

    @Test
    @DisplayName("읽기가 다르면 프롬프트도 달라진다")
    void differentReadingsProduceDifferentPrompts() {
        String namamono = openAIService.buildMeaningPrompt("生物", "生物", "ナマモノ", "名詞", "一般", "Korean");
        String seibutsu = openAIService.buildMeaningPrompt("生物", "生物", "セイブツ", "名詞", "一般", "Korean");

        assertThat(namamono).isNotEqualTo(seibutsu);
        // 동형이의어 예시 문구에는 두 읽기가 모두 등장하므로, 읽기를 알리는 줄로 비교한다.
        assertThat(namamono).contains("Reading (katakana): ナマモノ")
                .doesNotContain("Reading (katakana): セイブツ");
        assertThat(seibutsu).contains("Reading (katakana): セイブツ")
                .doesNotContain("Reading (katakana): ナマモノ");
    }

    // 확장이 보내는 reading 은 사전형이 아니라 자막에 뜬 활용형의 읽기다 (掛ける + カケ).
    // 이걸 사전형의 읽기인 것처럼 제시하면 모델이 모순으로 보고 UNKNOWN 을 뱉는다.
    @Test
    @DisplayName("활용형이면 읽기가 활용형의 것임을 밝히고, UNKNOWN 으로 새지 않게 못을 박는다")
    void inflectedFormReadingIsLabelled() {
        String prompt = openAIService.buildMeaningPrompt("掛ける", "掛け", "カケ", "動詞", "自立", "Korean");

        assertThat(prompt).contains("Dictionary form: 掛ける");
        assertThat(prompt).contains("Form seen in the subtitle: 掛け");
        assertThat(prompt).contains("Reading of that form (katakana): カケ");
        assertThat(prompt).contains("belongs to the inflected form");
        assertThat(prompt).contains("never answer UNKNOWN because of it");
    }

    @Test
    @DisplayName("활용형이 사전형과 같으면 활용형 안내를 붙이지 않는다")
    void skipsRedundantInflectedForm() {
        String prompt = openAIService.buildMeaningPrompt("生物", "生物", "ナマモノ", "名詞", "一般", "Korean");

        assertThat(prompt).doesNotContain("Form seen in the subtitle");
        assertThat(prompt).doesNotContain("belongs to the inflected form");
        assertThat(prompt).contains("Reading (katakana): ナマモノ");
    }

    @Test
    @DisplayName("품사는 선택 입력이라 없으면 줄 자체를 넣지 않는다")
    void omitsMissingPos() {
        String prompt = openAIService.buildMeaningPrompt("食べる", "食べる", "タベル", null, null, "Korean");

        assertThat(prompt).doesNotContain("Part of speech");
        assertThat(prompt).contains("タベル");
    }

    @Test
    @DisplayName("길이·형식 제약과 UNKNOWN 표식이 프롬프트에 있다")
    void promptStatesOutputConstraints() {
        String prompt = openAIService.buildMeaningPrompt("掛ける", "掛け", "カケ", "動詞", "自立", "Korean");

        assertThat(prompt).contains("1 to 3 senses separated by commas");
        assertThat(prompt).contains("no quotes, notes, romanization");
        assertThat(prompt).contains("UNKNOWN");
    }
}
