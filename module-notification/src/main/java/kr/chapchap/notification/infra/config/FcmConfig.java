package kr.chapchap.notification.infra.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


@ConditionalOnProperty(prefix = "chapchap.notification.fcm", name = "project-id")
@EnableConfigurationProperties(FcmProperties.class)
@Configuration
public class FcmConfig {
    @Bean
    public FirebaseApp firebaseApp(FcmProperties properties) throws IOException {
        try (InputStream credentialStream = resolveCredentialStream(properties)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialStream))
                    .setProjectId(properties.projectId())
                    .build();

            return FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private InputStream resolveCredentialStream(FcmProperties properties) throws IOException {
        if (properties.credentialsJson() != null && !properties.credentialsJson().isBlank()) {
            return new ByteArrayInputStream(properties.credentialsJson().getBytes(StandardCharsets.UTF_8));
        }
        return new FileInputStream(properties.credentialsFilePath());
    }
}
