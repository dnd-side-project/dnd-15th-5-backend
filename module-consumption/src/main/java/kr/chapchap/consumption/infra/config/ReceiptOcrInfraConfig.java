package kr.chapchap.consumption.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@EnableConfigurationProperties({ClovaOcrProperties.class, ReceiptImageStorageProperties.class})
@Configuration(proxyBeanMethods = false)
public class ReceiptOcrInfraConfig {

    @Bean
    public RestClient clovaOcrRestClient(
            RestClient.Builder builder,
            ClovaOcrProperties properties
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
