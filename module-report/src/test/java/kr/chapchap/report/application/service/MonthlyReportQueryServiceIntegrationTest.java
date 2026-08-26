package kr.chapchap.report.application.service;

import jakarta.persistence.EntityManager;
import kr.chapchap.core.test.JpaAuditingTestConfig;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.report.application.command.GetMonthlyReportCommand;
import kr.chapchap.report.application.info.MonthlyReportInfo;
import kr.chapchap.report.application.port.ConsumptionActivityPort;
import kr.chapchap.report.application.port.PlaceStickerLookupPort;
import kr.chapchap.report.application.port.UserNicknameLookupPort;
import kr.chapchap.report.domain.entity.Report;
import kr.chapchap.report.domain.repository.ReportCategoryStatRepository;
import kr.chapchap.report.domain.repository.ReportPlaceRankRepository;
import kr.chapchap.report.domain.repository.ReportRepository;
import kr.chapchap.report.domain.repository.ReportTimePatternRepository;
import kr.chapchap.report.domain.repository.ReportTownRankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


// 엔티티 <-> 실제 스키마 매핑과 인덱스 마이그레이션이 깨지지 않았는지 확인하는 통합 테스트.
@DataJpaTest
@EntityScan(basePackageClasses = Report.class)
@EnableJpaRepositories(basePackageClasses = ReportRepository.class)
@Import({TestcontainersConfiguration.class, JpaAuditingTestConfig.class})
class MonthlyReportQueryServiceIntegrationTest {

    // module-report는 @SpringBootApplication이 없는 라이브러리 모듈이라 @DataJpaTest가
    // 설정 클래스를 자동으로 못 찾는다. nested @Configuration을 두면 그걸로 대신 찾는다
    @Configuration(proxyBeanMethods = false)
    static class TestConfig {
    }

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportCategoryStatRepository reportCategoryStatRepository;

    @Autowired
    private ReportTownRankRepository reportTownRankRepository;

    @Autowired
    private ReportPlaceRankRepository reportPlaceRankRepository;

    @Autowired
    private ReportTimePatternRepository reportTimePatternRepository;

    @Autowired
    private EntityManager entityManager;

    // module-consumption의 실제 구현이 아니라 순수 Port라, DataJpaTest 컨텍스트에는 빈이 없어 직접 mock한다.
    private final ConsumptionActivityPort consumptionActivityPort = org.mockito.Mockito.mock(ConsumptionActivityPort.class);
    private final PlaceStickerLookupPort placeStickerLookupPort = org.mockito.Mockito.mock(PlaceStickerLookupPort.class);
    private final UserNicknameLookupPort userNicknameLookupPort = org.mockito.Mockito.mock(UserNicknameLookupPort.class);

    private MonthlyReportQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new MonthlyReportQueryService(
                reportRepository,
                reportCategoryStatRepository,
                reportTownRankRepository,
                reportPlaceRankRepository,
                reportTimePatternRepository,
                consumptionActivityPort,
                placeStickerLookupPort,
                userNicknameLookupPort
        );

        entityManager.createNativeQuery(
                "INSERT INTO users (id, nickname, status, created_at, updated_at) VALUES "
                        + "(1, '테스트유저', 'ACTIVE', now(), now())"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO places (id, name, road_address, administrative_dong_code, administrative_dong_name, location, created_at, updated_at) VALUES "
                        + "(101, '투썸 플레이스 뚝섬지점', '서울 성동구 어딘가', '1120000', '성수동', "
                        + "ST_SetSRID(ST_MakePoint(127.05, 37.54), 4326)::geography, now(), now())"
        ).executeUpdate();

        entityManager.createNativeQuery(
                "INSERT INTO report (id, user_id, report_month, persona_type, score_exploration, score_town_expansion, "
                        + "score_daytime, score_impulsive, total_visit_count, new_town_count, new_place_count, new_sticker_count, created_at) "
                        + "VALUES (1, 1, '2026-07-01', 'RHMP', 0.3, 0.2, 0.1, 0.8, 24, 5, 8, 3, now())"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO report_category_stat (report_id, category, percentage) VALUES "
                        + "(1, '카페', 60.0), (1, '먹거리', 25.0), (1, '놀거리', 15.0)"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO report_town_rank (report_id, rank, town_name, visit_count) VALUES "
                        + "(1, 1, '연남동', 5), (1, 2, '성수동', 4), (1, 3, '망원동', 2)"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO report_place_rank (report_id, rank, place_id, place_name, visit_count, first_visited_date) VALUES "
                        + "(1, 1, 101, '투썸 플레이스 뚝섬지점', 9, '2026-05-04')"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO report_time_pattern (report_id, day_of_week, visit_hour, visit_count) VALUES "
                        + "(1, 1, 20, 2), (1, 5, 22, 8), (1, 7, 18, 2)"
        ).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 실제_DB에_저장된_리포트를_조회하면_모든_필드가_정확히_매핑된다() {
        // given: 1위 가게(placeId=101)의 그 달 소비기록 - 카테고리는 여기서 즉석으로 조회한다
        org.mockito.Mockito.when(consumptionActivityPort.findActivities(
                        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new kr.chapchap.report.application.info.ConsumptionActivity(
                        101L, "카페", java.time.LocalDate.of(2026, 7, 3), null)));

        // when
        MonthlyReportInfo info = sut.getMonthlyReport(new GetMonthlyReportCommand(1L, YearMonth.of(2026, 7)));

        // then
        assertThat(info.reportId()).isEqualTo(1L);
        assertThat(info.persona().type()).isEqualTo("RHMP");
        assertThat(info.persona().typeName()).isEqualTo("골목 야간반장");
        assertThat(info.persona().scores().scoreExploration()).isEqualByComparingTo("0.30");

        assertThat(info.placeRanks()).hasSize(1);
        assertThat(info.placeRanks().get(0).placeName()).isEqualTo("투썸 플레이스 뚝섬지점");
        assertThat(info.placeRanks().get(0).firstVisitedDate()).isEqualTo("2026-05-04");
        assertThat(info.placeRanks().get(0).category()).isEqualTo("카페");

        assertThat(info.townRanks()).hasSize(3);
        assertThat(info.townRanks().get(0).townName()).isEqualTo("연남동");

        assertThat(info.summary().totalVisitCount()).isEqualTo(24);

        assertThat(info.categoryStats()).hasSize(3);

        assertThat(info.timePattern().peakDayOfWeek()).isEqualTo(5);
        assertThat(info.timePattern().peakTimeSlot()).isEqualTo("NIGHT");
        assertThat(info.timePattern().dayOfWeekPattern()).hasSize(7);
    }

    @Test
    void 없는_연월로_조회하면_예외가_발생한다() {
        assertThatThrownBy(() -> sut.getMonthlyReport(new GetMonthlyReportCommand(1L, YearMonth.of(2099, 1))))
                .isInstanceOf(kr.chapchap.core.exception.BusinessException.class);
    }
}
