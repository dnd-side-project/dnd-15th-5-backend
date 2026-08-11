package kr.chapchap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chapchap.aws.s3")
public record AwsS3Properties(String region) {
}
