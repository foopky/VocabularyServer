package vocabulary.app.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vocabulary.app.entity.User;
import vocabulary.app.repository.UserRepository;

// 예시: UserDetailsService 구현 클래스 (PrincipalDetailService)
@Service
@RequiredArgsConstructor
public class PrincipalDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    // 사용자 이름(ID)으로 DB에서 사용자 정보를 조회하여 UserDetails 객체로 랩핑하여 반환
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new PrincipalDetails(user);
    }
}