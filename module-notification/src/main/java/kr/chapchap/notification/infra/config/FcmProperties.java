package kr.chapchap.notification.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chapchap.notification.fcm")
public record FcmProperties (
        String projectId,
        String credentialsFilePath,
        String credentialsJson,
        String topicAllUsers
){
}

