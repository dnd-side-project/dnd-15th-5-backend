package kr.chapchap.consumption.infra.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.chapchap.consumption.application.info.PlacePopularityInfo;
import kr.chapchap.consumption.application.service.PopularityQueryService;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.repository.ConsumptionRepository;
import kr.chapchap.core.test.JpaAuditingTestConfig;
import kr.chapchap.core.test.TestcontainersConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker 실행 후 저장소 루트에서 명시적으로 실행한다(Gradle 캐시를 피하도록 --rerun-tasks 사용).
 * RUN_PLACE_CATEGORY_BENCHMARK=true ./gradlew :module-consumption:test --tests '*PlaceCategoryBenchmarkTest' --rerun-tasks
 * 결과: module-consumption/build/reports/benchmarks/place-category.json
 * Testcontainers DB의 실제 마이그레이션 스키마에 적재하며 테스트 종료 시 롤백한다.
 * 단일 요청·캐시 워밍 후의 측정으로, 운영 부하나 사진을 포함한 API 전체 시간을 의미하지 않는다.
 */
@Slf4j
@EnabledIfEnvironmentVariable(named = "RUN_PLACE_CATEGORY_BENCHMARK", matches = "true")
@DataJpaTest(showSql = false)
@EntityScan(basePackageClasses = Consumption.class)
@EnableJpaRepositories(basePackageClasses = ConsumptionRepository.class)
@Import({TestcontainersConfiguration.class, JpaAuditingTestConfig.class,
        ConsumptionQueryRepositoryImpl.class, PopularityQueryService.class,
        PlaceCategoryBenchmarkTest.QueryConfig.class})
class PlaceCategoryBenchmarkTest {

    private static final int MEASUREMENTS = 5;
    private static final Path REPORT_PATH = Path.of("build", "reports", "benchmarks", "place-category.json");

    @Configuration(proxyBeanMethods = false)
    static class QueryConfig {

        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PopularityQueryService popularityQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 백만건의_균등분포와_한_장소_십만건의_편중분포에서_카테고리_집계비용을_측정한다() throws Exception {
        // given
        seedMillionRecords();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM consumptions", Long.class)).isEqualTo(1_000_000L);

        // when
        List<Map<String, Object>> scenarios = new ArrayList<>();
        for (int placeCount : List.of(5, 100, 1000)) {
            List<Long> placeIds = LongStream.rangeClosed(1, placeCount).boxed().toList();
            scenarios.add(measure("uniform-" + placeCount + "-places", placeIds, 100L, 1_000_000L));
        }
        seedHotPlaceRecords();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM consumptions", Long.class)).isEqualTo(1_100_000L);
        scenarios.add(measure("hot-place-100k-records", List.of(10_001L), 100_000L, 1_100_000L));

        // then — 시간은 측정값만 남기고, 결과의 정확성은 각 시나리오에서 검증한다.
        Files.createDirectories(REPORT_PATH.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(REPORT_PATH.toFile(), Map.of(
                "databaseVersion", jdbc.queryForObject("SELECT version()", String.class),
                "conditions", "Synthetic data; warm cache; one warm-up and five measurements per scenario; "
                        + "single caller; DB query and Java service measured separately; not end-to-end API latency",
                "scenarios", scenarios
        ));
        log.info("카테고리 집계 측정 결과: {}", REPORT_PATH.toAbsolutePath());
    }

    private void seedMillionRecords() {
        assertThat(jdbc.queryForList("SELECT DISTINCT category FROM sticker_item", String.class))
                .containsExactlyInAnyOrder("카페", "운동", "편의점/마트", "취미/놀거리", "음식점", "미용/뷰티", "쇼핑", "기타");
        jdbc.update("""
                INSERT INTO users (id, nickname, status)
                SELECT i, '집계테스트', 'ACTIVE' FROM generate_series(1, 5000) i
                """);
        jdbc.update("""
                INSERT INTO places (id, name, road_address, administrative_dong_code,
                    administrative_dong_name, location)
                SELECT i, '집계테스트 장소', '테스트 주소', '1100000', '태평로',
                    ST_SetSRID(ST_MakePoint(126.9780, 37.5670), 4326)::geography
                FROM generate_series(1, 10001) i
                """);
        jdbc.update("""
                WITH categories AS (
                    SELECT category, min(id) AS sticker_id,
                        row_number() OVER (ORDER BY category COLLATE "C") - 1 AS category_index
                    FROM sticker_item GROUP BY category
                )
                INSERT INTO consumptions (purchase_date, purchase_time, amount, category,
                    user_id, place_id, sticker_item_id)
                SELECT CURRENT_DATE - (i % 365), TIME '12:00', 5000, c.category,
                    i % 5000 + 1, i % 10000 + 1, c.sticker_id
                FROM generate_series(0, 999999) i
                JOIN categories c ON c.category_index = (i / 10000) % 8
                """);
        jdbc.execute("ANALYZE consumptions");
    }

    private void seedHotPlaceRecords() {
        jdbc.update("""
                WITH categories AS (
                    SELECT category, min(id) AS sticker_id,
                        row_number() OVER (ORDER BY category COLLATE "C") - 1 AS category_index
                    FROM sticker_item GROUP BY category
                )
                INSERT INTO consumptions (purchase_date, purchase_time, amount, category,
                    user_id, place_id, sticker_item_id)
                SELECT CURRENT_DATE - (i % 365), TIME '12:00', 5000, c.category,
                    i % 5000 + 1, 10001, c.sticker_id
                FROM generate_series(0, 99999) i
                JOIN categories c ON c.category_index = i % 8
                """);
        jdbc.execute("ANALYZE consumptions");
    }

    private Map<String, Object> measure(
            String scenario,
            List<Long> placeIds,
            long recordsPerPlace,
            long totalRecords
    ) throws Exception {
        // Repository의 place_id·category GROUP BY SQL에 대응한다. 실제 서비스 결과도 별도로 검증한다.
        String ids = placeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String explainSql = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) "
                + "SELECT place_id, category, count(*), max(purchase_date) FROM consumptions "
                + "WHERE place_id IN (" + ids + ") GROUP BY place_id, category";
        popularityQueryService.aggregatePopularityByPlaceIds(placeIds);
        jdbc.queryForObject(explainSql, String.class);

        List<Double> serviceTimes = new ArrayList<>();
        List<Double> databaseTimes = new ArrayList<>();
        JsonNode lastPlan = null;
        for (int i = 0; i < MEASUREMENTS; i++) {
            long started = System.nanoTime();
            List<PlacePopularityInfo> result = popularityQueryService.aggregatePopularityByPlaceIds(placeIds);
            serviceTimes.add((System.nanoTime() - started) / 1_000_000.0);
            assertThat(result).extracting(PlacePopularityInfo::placeId).containsExactlyInAnyOrderElementsOf(placeIds);
            // 균등 분포는 이름순 앞의 네 카테고리가, 편중 분포는 여덟 카테고리가 동률이므로 기타를 선택한다.
            assertThat(result).allSatisfy(place -> {
                assertThat(place.visitCount()).isEqualTo(recordsPerPlace);
                assertThat(place.category()).isEqualTo("기타");
            });

            lastPlan = objectMapper.readTree(jdbc.queryForObject(explainSql, String.class)).get(0);
            databaseTimes.add(lastPlan.get("Execution Time").asDouble());
        }
        double serviceMedian = median(serviceTimes);
        double databaseMedian = median(databaseTimes);
        log.info("{}: 대상 기록 {}건, DB 중앙값 {}ms, 조회·대표 카테고리 계산 중앙값 {}ms",
                scenario, recordsPerPlace * placeIds.size(), databaseMedian, serviceMedian);
        return Map.of(
                "scenario", scenario,
                "totalRecords", totalRecords,
                "targetPlaces", placeIds.size(),
                "matchingRecords", recordsPerPlace * placeIds.size(),
                "databaseExecutionMs", Map.of("median", databaseMedian, "samples", databaseTimes),
                "serviceElapsedMs", Map.of("median", serviceMedian, "samples", serviceTimes),
                "explainAnalyze", lastPlan
        );
    }

    private double median(List<Double> values) {
        return values.stream().sorted().toList().get(values.size() / 2);
    }
}
