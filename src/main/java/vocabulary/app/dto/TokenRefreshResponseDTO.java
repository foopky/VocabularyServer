package vocabulary.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TokenRefreshResponseDTO {
    // 새로 발급된 Access Token
    private String jwt;
    // 회전된 새 Refresh Token (기존 토큰은 폐기되므로 반드시 교체 저장해야 함)
    private String refreshToken;
    private Long userId;
}
