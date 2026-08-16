package kr.chapchap.report.infra.config;

import kr.chapchap.report.domain.service.MonthlyAggregationCalculator;
import kr.chapchap.report.domain.service.PersonaAxisScoringService;
import kr.chapchap.report.domain.service.PersonaScoringService;
import kr.chapchap.report.domain.service.RecentDiscoveryMessageGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportBeanConfig {


    @Bean
    public RecentDiscoveryMessageGenerator recentDiscoveryMessageGenerator() {
        return new RecentDiscoveryMessageGenerator();
    }

    @Bean
    public MonthlyAggregationCalculator monthlyAggregationCalculator() {
        return new MonthlyAggregationCalculator();
    }

    @Bean
    public PersonaScoringService personaScoringService() {
        return new PersonaAxisScoringService();
    }
}
