package kr.chapchap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI chapChapOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Bearer JWT 인증입니다. "
                                                        + "POST /auth/signup/terms에서는 Signup Token을 사용하고, "
                                                        + "그 외 bearerAuth가 필요한 API에서는 Access Token을 사용합니다."
                                        )
                        )
                )
                .info(new Info()
                        .title("ChapChap API")
                        .description("ChapChap 백엔드 API 명세")
                        .version("current"));
    }
}
