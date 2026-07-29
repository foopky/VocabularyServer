package vocabulary.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

// 페이징 응답 공통 포맷
// Spring의 Page 객체를 그대로 직렬화하면 내부 구조가 바뀔 때 응답 스펙도 같이 흔들리므로
// (Spring Boot도 PageImpl 직렬화를 권장하지 않는다) 필요한 필드만 담아 고정된 형태로 내려준다.
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PageResponseDTO<T> {

    private List<T> content;
    // 현재 페이지 번호 (0부터 시작)
    private int page;
    // 페이지 당 개수
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
