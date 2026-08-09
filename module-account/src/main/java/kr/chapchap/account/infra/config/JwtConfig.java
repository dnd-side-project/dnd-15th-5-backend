package kr.chapchap.account.infra.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@EnableConfigurationProperties(JwtProperties.class)
@Configuration(proxyBeanMethods = false)
public class JwtConfig {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    @Bean
    public Clock jwtClock() {
        return Clock.systemUTC();
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        SecretKey secretKey = createSecretKey(properties.secret());
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties, Clock jwtClock) {
        SecretKey secretKey = createSecretKey(properties.secret());
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(jwtClock);
        jwtDecoder.setJwtValidator(timestampValidator);
        return jwtDecoder;
    }

    private SecretKey createSecretKey(String secret) {
        return new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA_256
        );
    }
}
