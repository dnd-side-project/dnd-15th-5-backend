package kr.chapchap.report;

import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.report.application.command.AggregateMonthlyReportCommand;
import kr.chapchap.report.application.command.GetMonthlyReportCommand;
import kr.chapchap.report.application.info.MonthlyReportAggregationResultInfo;
import kr.chapchap.report.application.info.MonthlyReportInfo;
import kr.chapchap.report.application.service.MonthlyReportAggregationService;
import kr.chapchap.report.application.service.MonthlyReportQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

// consumptions 원본 데이터를 집계해 report* 테이블을 채우고,
// 이미 구현되어 있던 조회 API(MonthlyReportQueryService)가 그 결과를 정확히 읽어오는지 엔드투엔드로 검증한다.
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MonthlyReportBatchIntegrationTest {

    private final MonthlyReportAggregationService monthlyReportAggregationService;
    private final MonthlyReportQueryService monthlyReportQueryService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    MonthlyReportBatchIntegrationTest(
            MonthlyReportAggregationService monthlyReportAggregationService,
            MonthlyReportQueryService monthlyReportQueryService,
            JdbcTemplate jdbcTemplate
    ) {
        this.monthlyReportAggregationService = monthlyReportAggregationService;
        this.monthlyReportQueryService = monthlyReportQueryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void seedConsumptionData() {
        // 같은 컨테이너 DB를 테스트 메서드끼리 공유하므로 매번 이전 데이터를 정리한다.
        jdbcTemplate.update("DELETE FROM report");
        jdbcTemplate.update("DELETE FROM consumptions");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update(
                "INSERT INTO users (id, nickname, status, created_at, updated_at) VALUES (1, '테스트유저', 'ACTIVE', now(), now())");

        jdbcTemplate.update(
                "INSERT INTO places (id, name, road_address, administrative_dong_code, administrative_dong_name, location, created_at, updated_at) VALUES "
                        + "(101, '카페 성수', '서울 성동구 어딘가', '1120000', '성수동', "
                        + "ST_SetSRID(ST_MakePoint(127.05, 37.54), 4326)::geography, now(), now()), "
                        + "(102, '연남 식당', '서울 마포구 어딘가', '1130000', '연남동', "
                        + "ST_SetSRID(ST_MakePoint(126.92, 37.56), 4326)::geography, now(), now())");

        // 2026년 7월: place 101(성수동, CAFE) 2회, place 102(연남동, RESTAURANT) 1회
        jdbcTemplate.update(
                "INSERT INTO consumptions (purchase_date, purchase_time, amount, category, user_id, place_id, created_at, updated_at) VALUES "
                        + "('2026-07-06', '20:00:00', 8000, 'CAFE', 1, 101, now(), now()), "
                        + "('2026-07-13', '10:00:00', 6000, 'CAFE', 1, 101, now(), now()), "
                        + "('2026-07-20', '12:30:00', 15000, 'RESTAURANT', 1, 102, now(), now())");
    }

    @Test
    void 배치를_실행하면_소비_데이터를_집계해_월간_리포트_조회_API로_읽을_수_있다() {
        // when
        MonthlyReportAggregationResultInfo aggregationResult =
                monthlyReportAggregationService.aggregate(AggregateMonthlyReportCommand.forAllActiveUsers(YearMonth.of(2026, 7)));

        // then
        assertThat(aggregationResult.lockAcquired()).isTrue();
        assertThat(aggregationResult.succeededCount()).isEqualTo(1);
        assertThat(aggregationResult.failedUserIds()).isEmpty();

        MonthlyReportInfo report = monthlyReportQueryService.getMonthlyReport(new GetMonthlyReportCommand(1L, YearMonth.of(2026, 7)));

        assertThat(report.summary().totalVisitCount()).isEqualTo(3);
        assertThat(report.summary().newTownCount()).isEqualTo(2);
        assertThat(report.summary().newPlaceCount()).isEqualTo(2);

        assertThat(report.townRanks()).extracting("townName").containsExactlyInAnyOrder("성수동", "연남동");
        assertThat(report.placeRanks()).extracting("placeName").containsExactlyInAnyOrder("카페 성수", "연남 식당");

        assertThat(report.categoryStats())
                .anySatisfy(stat -> {
                    assertThat(stat.category()).isEqualTo("CAFE");
                    assertThat(stat.percentage()).isEqualByComparingTo("66.67");
                });
    }

    @Test
    void 같은_달을_두_번_집계해도_리포트가_중복_생성되지_않는다() {
        // given
        monthlyReportAggregationService.aggregate(AggregateMonthlyReportCommand.forAllActiveUsers(YearMonth.of(2026, 7)));

        // when
        monthlyReportAggregationService.aggregate(AggregateMonthlyReportCommand.forAllActiveUsers(YearMonth.of(2026, 7)));

        // then
        Integer reportCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report WHERE user_id = 1 AND report_month = '2026-07-01'", Integer.class);
        assertThat(reportCount).isEqualTo(1);

        Integer categoryStatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report_category_stat rcs JOIN report r ON r.id = rcs.report_id "
                        + "WHERE r.user_id = 1 AND r.report_month = '2026-07-01'", Integer.class);
        assertThat(categoryStatCount).isEqualTo(2);
    }
}
