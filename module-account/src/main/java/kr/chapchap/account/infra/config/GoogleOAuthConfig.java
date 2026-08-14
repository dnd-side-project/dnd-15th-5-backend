package kr.chapchap.account.infra.config;

import kr.chapchap.account.infra.external.google.GoogleAuthenticationClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;

@EnableConfigurationProperties(GoogleOAuthProperties.class)
@Configuration(proxyBeanMethods = false)
public class GoogleOAuthConfig {

    @Bean
    public RestClient googleRestClient(
            RestClient.Builder builder,
            GoogleOAuthProperties properties
    ) {
        return builder
                .requestFactory(createRequestFactory(properties))
                .build();
    }

    @Bean
    public GoogleAuthenticationClient googleAuthenticationClient(
            @Qualifier("googleRestClient") RestClient googleRestClient,
            GoogleOAuthProperties properties
    ) {
        NimbusJwtDecoder idTokenDecoder = NimbusJwtDecoder
                .withJwkSetUri(properties.jwkSetUri().toString())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(new RestTemplate(createRequestFactory(properties)))
                .build();
        idTokenDecoder.setJwtValidator(new JwtTimestampValidator());

        return new GoogleAuthenticationClient(
                googleRestClient,
                properties,
                idTokenDecoder
        );
    }

    private JdkClientHttpRequestFactory createRequestFactory(
            GoogleOAuthProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
    }
}
