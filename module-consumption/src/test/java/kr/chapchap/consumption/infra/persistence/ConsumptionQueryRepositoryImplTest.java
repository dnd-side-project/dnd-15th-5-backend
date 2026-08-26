package kr.chapchap.consumption.infra.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.chapchap.core.test.JpaAuditingTestConfig;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.consumption.domain.entity.Consumption;
import kr.chapchap.consumption.domain.entity.PlaceCategoryVisitRow;
import kr.chapchap.consumption.domain.repository.ConsumptionQueryRepository;
import kr.chapchap.consumption.domain.repository.ConsumptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

// 테스트 클래스가 domain.repository/domain.entity와 다른 가지(infra.persistence)에 있어서
// @DataJpaTest의 기본 스캔 범위 밖이다. 명시적으로 지정해줘야 ConsumptionRepository/Consumption을 찾는다.
@DataJpaTest
@EntityScan(basePackageClasses = Consumption.class)
@EnableJpaRepositories(basePackageClasses = ConsumptionRepository.class)
@Import({TestcontainersConfiguration.class, JpaAuditingTestConfig.class, ConsumptionQueryRepositoryImpl.class,
        ConsumptionQueryRepositoryImplTest.QuerydslTestConfig.class})
class ConsumptionQueryRepositoryImplTest {

    @Configuration(proxyBeanMethods = false)
    static class QuerydslTestConfig {

        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }

    @Autowired
    private ConsumptionRepository consumptionRepository;

    @Autowired
    private ConsumptionQueryRepository consumptionQueryRepository;

    @Autowired
    private EntityManager entityManager;

    // consumptions.user_id/place_id는 users/places를 참조하는 물리 FK라 미리 존재해야 한다.
    // User/Place 엔티티는 각각 module-account/module-place 소유라 여기서 직접 쓸 수 없어 네이티브 SQL로 최소 fixture만 넣는다.
    @BeforeEach
    void seedForeignKeyFixtures() {
        entityManager.createNativeQuery(
                "INSERT INTO users (id, nickname, status, created_at, updated_at) VALUES "
                        + "(1, '테스트유저1', 'ACTIVE', now(), now()), "
                        + "(2, '테스트유저2', 'ACTIVE', now(), now())"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO places "
                        + "(id, name, road_address, administrative_dong_code, administrative_dong_name, location, created_at, updated_at) "
                        + "VALUES "
                        + "(101, '테스트 가게', '서울 성동구 테스트로 1', '1120510100', '성수동', "
                        + "ST_SetSRID(ST_MakePoint(127.0557, 37.5447), 4326), now(), now()), "
                        + "(102, '테스트 가게2', '서울 강남구 테스트로 2', '1168010700', '역삼동', "
                        + "ST_SetSRID(ST_MakePoint(127.0364, 37.4999), 4326), now(), now()), "
                        + "(103, '테스트 가게3', '서울 마포구 테스트로 3', '1144010100', '아현동', "
                        + "ST_SetSRID(ST_MakePoint(126.9565, 37.5574), 4326), now(), now())"
        ).executeUpdate();
    }

    @Test
    void 첫_조회는_커서_없이_최신순으로_가져온다() {
        // given
        Consumption julyLate = createConsumption(1L, LocalDate.of(2026, 7, 20), LocalTime.of(20, 0));
        Consumption julyEarly = createConsumption(1L, LocalDate.of(2026, 7, 5), LocalTime.of(12, 0));
        Consumption june = createConsumption(1L, LocalDate.of(2026, 6, 20), LocalTime.of(20, 0));
        Consumption otherUser = createConsumption(2L, LocalDate.of(2026, 7, 10), LocalTime.of(20, 0));
        consumptionRepository.saveAll(List.of(julyLate, julyEarly, june, otherUser));

        // when
        List<Consumption> result = consumptionQueryRepository.searchByCursor(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1),
                null, null, null,
                10
        );

        // then
        assertThat(result)
                .extracting(Consumption::getPurchaseDate)
                .containsExactly(
                        LocalDate.of(2026, 7, 20),
                        LocalDate.of(2026, 7, 5)
                );
    }

    @Test
    void 커서_이후_항목만_이어서_가져온다() {
        // given
        Consumption c1 = createConsumption(1L, LocalDate.of(2026, 7, 20), LocalTime.of(20, 0));
        Consumption c2 = createConsumption(1L, LocalDate.of(2026, 7, 15), LocalTime.of(20, 0));
        Consumption c3 = createConsumption(1L, LocalDate.of(2026, 7, 5), LocalTime.of(12, 0));
        consumptionRepository.saveAll(List.of(c1, c2, c3));

        // when: c2를 마지막으로 본 커서로 다음 페이지 요청
        List<Consumption> result = consumptionQueryRepository.searchByCursor(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1),
                c2.getPurchaseDate(), c2.getPurchaseTime(), c2.getId(),
                10
        );

        // then: c2보다 과거인 c3만 남아야 함
        assertThat(result)
                .extracting(Consumption::getPurchaseDate)
                .containsExactly(LocalDate.of(2026, 7, 5));
    }

    @Test
    void fetchSize만큼만_가져온다() {
        // given
        for (int day = 1; day <= 5; day++) {
            consumptionRepository.save(createConsumption(1L, LocalDate.of(2026, 7, day), LocalTime.of(12, 0)));
        }

        // when: 
        List<Consumption> result = consumptionQueryRepository.searchByCursor(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1),
                null, null, null,
                3
        );

        // then
        assertThat(result).hasSize(3);
    }

    @Test
    void 장소별_최신_소비기록을_조회할_때_해당_사용자의_최근_방문순으로_반환한다() {
        // given
        Consumption oldPlace101 = createConsumption(
                1L, 101L, "카페", LocalDate.of(2026, 8, 1), LocalTime.of(9, 0)
        );
        Consumption latestPlace101 = createConsumption(
                1L, 101L, "카페", LocalDate.of(2026, 8, 20), LocalTime.of(18, 0)
        );
        Consumption latestPlace102 = createConsumption(
                1L, 102L, "음식점", LocalDate.of(2026, 8, 15), LocalTime.of(12, 0)
        );
        Consumption otherUser = createConsumption(
                2L, 103L, "카페", LocalDate.of(2026, 8, 21), LocalTime.of(20, 0)
        );
        consumptionRepository.saveAllAndFlush(List.of(
                oldPlace101, latestPlace101, latestPlace102, otherUser
        ));

        // when
        List<Consumption> result = consumptionQueryRepository.searchLatestVisitedPlacesByCursor(
                1L, null, null, null, 10
        );

        // then
        assertThat(result)
                .extracting(Consumption::getId, Consumption::getPlaceId)
                .containsExactly(
                        tuple(latestPlace101.getId(), 101L),
                        tuple(latestPlace102.getId(), 102L)
                );
    }

    @Test
    void 장소별_최신_소비기록을_조회할_때_방문일시가_같으면_ID_내림차순으로_정렬해_cursor_이후_기록을_반환한다() {
        // given
        LocalDate purchaseDate = LocalDate.of(2026, 8, 21);
        LocalTime purchaseTime = LocalTime.of(12, 30);
        Consumption firstSaved = createConsumption(1L, 101L, "카페", purchaseDate, purchaseTime);
        Consumption secondSaved = createConsumption(1L, 102L, "카페", purchaseDate, purchaseTime);
        Consumption thirdSaved = createConsumption(1L, 103L, "카페", purchaseDate, purchaseTime);
        consumptionRepository.saveAllAndFlush(List.of(firstSaved, secondSaved, thirdSaved));

        List<Consumption> firstPage = consumptionQueryRepository.searchLatestVisitedPlacesByCursor(
                1L, null, null, null, 2
        );

        // when
        Consumption cursor = firstPage.get(firstPage.size() - 1);
        List<Consumption> secondPage = consumptionQueryRepository.searchLatestVisitedPlacesByCursor(
                1L,
                cursor.getPurchaseDate(),
                cursor.getPurchaseTime(),
                cursor.getId(),
                2
        );

        // then
        assertThat(firstPage).extracting(Consumption::getId)
                .containsExactly(thirdSaved.getId(), secondSaved.getId());
        assertThat(secondPage).extracting(Consumption::getId)
                .containsExactly(firstSaved.getId());
    }

    @Test
    void 장소별로_소비건수를_집계해서_방문횟수가_많은_순으로_반환한다() {
        // given
        consumptionRepository.saveAll(List.of(
                createConsumption(1L, 101L, "카페", LocalDate.of(2026, 7, 1), LocalTime.of(12, 0)),
                createConsumption(1L, 101L, "카페", LocalDate.of(2026, 7, 8), LocalTime.of(12, 0)),
                createConsumption(1L, 102L, "음식점", LocalDate.of(2026, 7, 3), LocalTime.of(19, 0))
        ));

        // when
        List<PlaceCategoryVisitRow> result = consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null);

        // then
        assertThat(result)
                .extracting(PlaceCategoryVisitRow::placeId, PlaceCategoryVisitRow::visitCount)
                .containsExactly(
                        tuple(101L, 2L),
                        tuple(102L, 1L)
                );
    }

    @Test
    void 카테고리_필터를_주면_해당_카테고리의_장소만_집계한다() {
        // given
        consumptionRepository.saveAll(List.of(
                createConsumption(1L, 101L, "카페", LocalDate.of(2026, 7, 1), LocalTime.of(12, 0)),
                createConsumption(1L, 102L, "음식점", LocalDate.of(2026, 7, 3), LocalTime.of(19, 0))
        ));

        // when
        List<PlaceCategoryVisitRow> result =
                consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, List.of("카페"));

        // then
        assertThat(result)
                .extracting(PlaceCategoryVisitRow::placeId)
                .containsExactly(101L);
    }

    @Test
    void 다른_유저의_소비기록은_집계에_포함되지_않는다() {
        // given
        consumptionRepository.saveAll(List.of(
                createConsumption(1L, 101L, "카페", LocalDate.of(2026, 7, 1), LocalTime.of(12, 0)),
                createConsumption(2L, 102L, "음식점", LocalDate.of(2026, 7, 3), LocalTime.of(19, 0))
        ));

        // when
        List<PlaceCategoryVisitRow> result = consumptionQueryRepository.aggregateVisitedPlacesByCategory(1L, null);

        // then
        assertThat(result)
                .extracting(PlaceCategoryVisitRow::placeId)
                .containsExactly(101L);
    }

    private Consumption createConsumption(Long userId, LocalDate purchaseDate, LocalTime purchaseTime) {
        return createConsumption(userId, 101L, "카페", purchaseDate, purchaseTime);
    }

    private Consumption createConsumption(Long userId, Long placeId, String category,
                                           LocalDate purchaseDate, LocalTime purchaseTime) {
        return Consumption.builder()
                .userId(userId)
                .placeId(placeId)
                .category(category)
                .amount(5000L)
                .purchaseDate(purchaseDate)
                .purchaseTime(purchaseTime)
                .stickerItemId(1L)
                .build();
    }
}
