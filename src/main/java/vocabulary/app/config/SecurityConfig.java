package vocabulary.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vocabulary.app.jwt.JwtAuthenticationEntryPoint;
import vocabulary.app.jwt.JwtAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // vocabulary.app.jwt.JwtAuthenticationFilter: JWT 토큰을 인증하고 SecurityContextHolder에 추가하는 필터
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationConfiguration authenticationConfiguration;
    // 인증 실패 시 403 대신 401 + 사유(TOKEN_EXPIRED 등)를 내려주는 핸들러
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 기본적으로 스프링 시큐리티는 모든 URL을 보호함

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 🚨 인증 및 인가 설정 수정 🚨
                // permitAll() 모두 허용
                // 특정 http 메서드에 대해 지정할 때 HttpMethod.POST
                .authorizeHttpRequests(auth -> auth
                        // 에러 디스패치(/error)는 인증 대상이 아니다.
                        // 막아두면 서버 오류나 404가 전부 401로 둔갑해서, 프론트가 "세션 만료"로 오해하고 로그아웃시킨다.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/auth/login").permitAll()
                        // Access Token이 만료된 상태에서 호출하는 API이므로 인증 없이 허용해야 한다.
                        .requestMatchers(HttpMethod.POST,"/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/users").permitAll().
                                requestMatchers(HttpMethod.GET, "/healthCheck").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/users").permitAll()
//                        .requestMatchers("/**").permitAll() // 모든 경로 허용
                        .anyRequest().authenticated()
                )
                // 인증 실패 시 401 + JSON 응답 (프론트가 만료를 감지해 재발급을 시도할 수 있도록)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                // 이거 안하면 실행안됨
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class // 🎯 이 필터보다 반드시 먼저 실행되어야 합니다.
                );

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:[*]", "https://japflix-web.foopky.com", "https://voca-app-backend.foopky.com", "https://www.netflix.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
