package kr.chapchap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class CorsConfig {

    private static final String ALL = "*";
    private static final String ALL_PATHS = "/**";

    @Bean
    @ConfigurationProperties(prefix = "chapchap.cors")
    public CorsConfiguration corsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(List.of(ALL));
        configuration.setAllowedHeaders(List.of(ALL));
        configuration.setAllowCredentials(true);
        return configuration;
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            CorsConfiguration corsConfiguration
    ) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ALL_PATHS, corsConfiguration);
        return source;
    }
}
