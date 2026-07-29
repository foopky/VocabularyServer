package vocabulary.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// POST /api/users 요청 본문 (회원가입)
// role은 받지 않는다. 클라이언트가 보내는 값은 무시하고 서버가 항상 USER로 고정한다.
// (엔티티를 그대로 받던 시절, "role": 1 이 ordinal로 해석되어 ADMIN 계정이 만들어졌다)
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserSignupRequestDTO {
    private String name;
    private String password;
    private String description;
}
