package kr.chapchap.consumption.infra.config;

import kr.chapchap.consumption.domain.service.PlaceVisitCommentGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumptionBeanConfig {

    @Bean
    public PlaceVisitCommentGenerator placeVisitCommentGenerator() {
        return new PlaceVisitCommentGenerator();
    }
}
