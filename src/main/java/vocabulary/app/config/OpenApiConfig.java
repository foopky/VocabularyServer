package vocabulary.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 사용할 보안 스키마의 이름 정의
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName)) // 전역적으로 보안 설정 적용
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP) // HTTP 방식 사용
                                .scheme("bearer")             // 토큰 유형: Bearer
                                .bearerFormat("JWT")          // 토큰 형식: JWT
                                .in(SecurityScheme.In.HEADER) // 토큰을 HTTP 헤더를 통해 전달
                        )
                );
    }
}