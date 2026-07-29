package vocabulary.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// PUT /api/users/{userId}/password 요청 본문
// 비밀번호는 이 API로만 변경한다. (PUT /api/users/{userId} 는 더 이상 password를 받지 않는다)
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChangePasswordRequestDTO {
    private String currentPassword;
    private String newPassword;
}
