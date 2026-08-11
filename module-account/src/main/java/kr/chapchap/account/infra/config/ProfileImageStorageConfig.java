package kr.chapchap.account.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(ProfileImageStorageProperties.class)
@Configuration(proxyBeanMethods = false)
public class ProfileImageStorageConfig {
}
