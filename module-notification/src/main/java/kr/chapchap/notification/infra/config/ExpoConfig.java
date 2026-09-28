package kr.chapchap.notification.infra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@ConditionalOnProperty(prefix = "chapchap.notification.expo", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ExpoProperties.class)
@Configuration(proxyBeanMethods = false)
public class ExpoConfig {

    private static final String EXPO_PUSH_API_BASE_URL = "https://exp.host/--/api/v2/push/send";

    @Bean
    public RestClient expoRestClient(
            RestClient.Builder builder,
            ExpoProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient.Builder restClientBuilder = builder
                .baseUrl(EXPO_PUSH_API_BASE_URL)
                .requestFactory(requestFactory);

        if (StringUtils.hasText(properties.accessToken())) {
            restClientBuilder.defaultHeader("Authorization", "Bearer " + properties.accessToken());
        }

        return restClientBuilder.build();
    }
}
