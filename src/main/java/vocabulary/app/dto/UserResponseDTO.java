package vocabulary.app.dto;

import lombok.Getter;
import vocabulary.app.entity.Role;
import vocabulary.app.entity.User;

// 사용자 응답 본문. 비밀번호 해시는 절대 포함하지 않는다.
@Getter
public class UserResponseDTO {
    private final Long id;
    private final String name;
    private final Role role;
    private final String description;

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.role = user.getRole();
        this.description = user.getDescription();
    }
}
