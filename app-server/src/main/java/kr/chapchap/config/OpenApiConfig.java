package kr.chapchap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI chapChapOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChapChap API")
                        .description("ChapChap 백엔드 API 명세")
                        .version("current"));
    }
}
