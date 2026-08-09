package kr.chapchap.report.infra.config;

import kr.chapchap.report.domain.service.RecentDiscoveryMessageGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReportBeanConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public RecentDiscoveryMessageGenerator recentDiscoveryMessageGenerator() {
        return new RecentDiscoveryMessageGenerator();
    }
}
