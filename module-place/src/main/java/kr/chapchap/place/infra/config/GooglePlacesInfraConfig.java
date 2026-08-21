package kr.chapchap.place.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@EnableConfigurationProperties(GooglePlacesProperties.class)
@Configuration(proxyBeanMethods = false)
public class GooglePlacesInfraConfig {

    @Bean
    public RestClient googlePlacesRestClient(
            RestClient.Builder builder,
            GooglePlacesProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl(properties.baseUri().toString())
                .defaultHeader("X-Goog-Api-Key", properties.apiKey())
                .requestFactory(requestFactory)
                .build();
    }
}
