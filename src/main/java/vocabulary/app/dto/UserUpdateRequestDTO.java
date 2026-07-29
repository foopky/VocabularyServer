package vocabulary.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// PUT /api/users/{userId} 요청 본문 (프로필 수정)
// 엔티티를 그대로 받으면 password/role/id 까지 클라이언트가 덮어쓸 수 있으므로 수정 가능한 필드만 노출한다.
// - password: ChangePasswordRequestDTO 로 분리 (평문 저장 사고 방지)
// - role: 서버에서만 결정
// - name: JWT의 subject(사용자 이름)라서 변경하면 발급된 토큰/소유권 검사가 전부 깨진다. 지원하지 않음
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserUpdateRequestDTO {
    private String description;
}
