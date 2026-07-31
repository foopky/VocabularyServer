package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vocabulary.app.entity.WordMeaningCache;

import java.util.Optional;

public interface WordMeaningCacheRepository extends JpaRepository<WordMeaningCache, Long> {

    // 캐시 키 (base_form, reading, pos, target_language) 로 조회한다.
    Optional<WordMeaningCache> findByBaseFormAndReadingAndPosAndTargetLanguage(
            String baseForm, String reading, String pos, String targetLanguage);
}
