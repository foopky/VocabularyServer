package vocabulary.app.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // 토큰 검증 결과. 프론트가 "만료"와 "위조"를 구분할 수 있어야 재발급 여부를 판단할 수 있다.
    public enum ValidationResult {
        VALID,
        EXPIRED,
        INVALID
    }

    // 시크릿 키는 application.properties 등에 안전하게 저장
    @Value("${jwt.secret}")
    private String secretKey;

    // Access Token 유효 시간 (기본 1시간)
    // 탈취 시 피해를 줄이기 위해 짧게 두고, 만료되면 Refresh Token으로 재발급받는다.
    @Value("${jwt.access-token-validity-ms:3600000}")
    private long accessTokenValidityMs;

    // 키 초기화 (객체 생성 후 한 번만 실행)
    private Key key;

    @PostConstruct
    protected void init() {
        // 비밀 키를 Base64 인코딩하여 Key 객체로 변환
        String base64Secret = Base64.getEncoder().encodeToString(secretKey.getBytes());
        this.key = Keys.hmacShaKeyFor(base64Secret.getBytes());
    }

    // 1. JWT 토큰 생성
    public String createToken(Authentication authentication) {
        return createToken(authentication.getName()); // UserDetails의 getUsername() 값
    }

    // Refresh Token으로 재발급할 때는 Authentication 객체 없이 사용자 이름만으로 생성한다.
    public String createToken(String username) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .setSubject(username) // 토큰 주체 (사용자 ID)
                .setIssuedAt(now) // 발급 시간
                .setExpiration(validity) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 사용할 서명 알고리즘 및 시크릿 키
                .compact(); // 토큰 생성
    }

    // 2. JWT 토큰 검증
    public boolean validateToken(String token) {
        return validate(token) == ValidationResult.VALID;
    }

    // 만료(EXPIRED)와 그 외 오류(INVALID)를 구분해서 반환
    public ValidationResult validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return ValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            // 만료된 JWT 토큰 -> 클라이언트가 Refresh Token으로 재발급을 시도해야 한다
            return ValidationResult.EXPIRED;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            // 잘못된 JWT 서명
        } catch (UnsupportedJwtException e) {
            // 지원되지 않는 JWT 토큰
        } catch (IllegalArgumentException e) {
            // JWT 클레임이 비어있음
        }
        return ValidationResult.INVALID;
    }

    // 3. 토큰에서 인증 정보(Authentication) 추출
    public Authentication getAuthentication(String token) {
        // 토큰에서 클레임(정보) 추출
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        // DB를 조회하지 않고 토큰의 정보로만 인증 객체 생성
        // TODO: 실제 구현 시에는 Claims의 ID를 이용해 UserDetailsService로 DB에서 사용자 정보를 로드하는 것이 일반적

        // 예시: 권한 정보를 추출 (Claims에 권한 정보를 넣었다고 가정)
        Collection<? extends GrantedAuthority> authorities = Collections.emptyList(); // 권한 추출 로직 필요

        // UserDetails 객체 생성
        UserDetails principal = new User(claims.getSubject(), "", authorities);

        // Spring Security의 Authentication 객체 반환
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
}