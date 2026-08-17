package kr.chapchap.place.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@EnableConfigurationProperties(SgisProperties.class)
@Configuration(proxyBeanMethods = false)
public class SgisInfraConfig {

    @Bean
    public RestClient sgisRestClient(
            RestClient.Builder builder,
            SgisProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .requestFactory(requestFactory)
                .build();
    }
}
