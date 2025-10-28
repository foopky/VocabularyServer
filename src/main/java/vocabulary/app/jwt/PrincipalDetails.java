package vocabulary.app.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vocabulary.app.entity.User;
import java.util.Collection;
import java.util.List;

// 예시: UserDetails 구현 클래스 (PrincipalDetails)
public class PrincipalDetails implements UserDetails {
    private final User user; // 실제 DB의 사용자 엔티티

    public PrincipalDetails(User user) {
        this.user = user;
    }

    // 사용자의 권한(Role)을 반환 (ROLE_USER, ROLE_ADMIN 등)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 권한을 GrantedAuthority 리스트로 변환하여 반환
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword(); // 암호화된 비밀번호
    }

    @Override
    public String getUsername() {
        return user.getName(); // 사용자 ID
    }

    // ... 계정 만료, 잠김 등 나머지 메서드 구현
}


