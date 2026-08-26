package kr.chapchap.recommendation;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.recommendation.application.info.RecommendationInfo;
import kr.chapchap.recommendation.application.info.RecommendedPlaceInfo;
import kr.chapchap.recommendation.application.service.RecommendationQueryService;
import kr.chapchap.recommendation.exception.RecommendationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

// 별도 rollup 테이블·캐시 없이, module-place(반경 검색)와 module-consumption(인기도 집계)
// 두 모듈의 Port를 module-recommendation이 실제로 조합해서 올바른 결과를 만드는지 엔드투엔드로 검증한다.
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RecommendationIntegrationTest {

    private final RecommendationQueryService recommendationQueryService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    RecommendationIntegrationTest(RecommendationQueryService recommendationQueryService, JdbcTemplate jdbcTemplate) {
        this.recommendationQueryService = recommendationQueryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM consumptions");
        jdbcTemplate.update("DELETE FROM place_likes");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update(
                "INSERT INTO users (id, nickname, status, created_at, updated_at) VALUES "
                        + "(1, '테스트유저', 'ACTIVE', now(), now()), "
                        + "(2, '다른유저', 'ACTIVE', now(), now())");

        // 서울시청(37.5665, 126.9780) 기준 반경 1km 안
        // 반경 밖(부산): 103
        jdbcTemplate.update(
                "INSERT INTO places (id, name, road_address, administrative_dong_code, administrative_dong_name, location, created_at, updated_at) VALUES "
                        + "(101, '반경안 카페', '서울 중구 어딘가', '1100000', '태평로', "
                        + "ST_SetSRID(ST_MakePoint(126.9780, 37.5670), 4326)::geography, now(), now()), "
                        + "(102, '반경안 식당', '서울 중구 어딘가', '1100000', '태평로', "
                        + "ST_SetSRID(ST_MakePoint(126.9790, 37.5660), 4326)::geography, now(), now()), "
                        + "(104, '반경안 카페2', '서울 중구 어딘가', '1100000', '태평로', "
                        + "ST_SetSRID(ST_MakePoint(126.9775, 37.5668), 4326)::geography, now(), now()), "
                        + "(105, '반경안 카페3', '서울 중구 어딘가', '1100000', '태평로', "
                        + "ST_SetSRID(ST_MakePoint(126.9782, 37.5663), 4326)::geography, now(), now()), "
                        + "(103, '반경밖 가게', '부산 어딘가', '2600000', '중앙동', "
                        + "ST_SetSRID(ST_MakePoint(129.0756, 35.1796), 4326)::geography, now(), now())");

        jdbcTemplate.update(
                "INSERT INTO consumptions (purchase_date, purchase_time, amount, category, user_id, place_id, sticker_item_id, created_at, updated_at) VALUES "
                        // 101: 카페, 5회 방문 (myTownPlaces 1등)
                        + "('2026-08-01', '09:00:00', 5000, '카페', 2, 101, 1, now(), now()), "
                        + "('2026-08-02', '09:00:00', 5000, '카페', 2, 101, 1, now(), now()), "
                        + "('2026-08-03', '09:00:00', 5000, '카페', 2, 101, 1, now(), now()), "
                        + "('2026-08-04', '09:00:00', 5000, '카페', 2, 101, 1, now(), now()), "
                        + "('2026-08-05', '09:00:00', 5000, '카페', 2, 101, 1, now(), now()), "
                        // 102: 음식점, 4회 방문 (myTownPlaces 2등)
                        + "('2026-08-01', '12:00:00', 12000, '음식점', 2, 102, 1, now(), now()), "
                        + "('2026-08-02', '12:00:00', 12000, '음식점', 2, 102, 1, now(), now()), "
                        + "('2026-08-03', '12:00:00', 12000, '음식점', 2, 102, 1, now(), now()), "
                        + "('2026-08-04', '12:00:00', 12000, '음식점', 2, 102, 1, now(), now()), "
                        // 104: 카페, 3회 방문 (myTownPlaces엔 안 들고, 카페 중에선 101 다음 순위라 sameCategoryPlaces에 남아야 함)
                        + "('2026-08-01', '10:00:00', 4500, '카페', 2, 104, 1, now(), now()), "
                        + "('2026-08-02', '10:00:00', 4500, '카페', 2, 104, 1, now(), now()), "
                        + "('2026-08-03', '10:00:00', 4500, '카페', 2, 104, 1, now(), now()), "
                        // 103: 반경 밖인데 방문횟수는 훨씬 많음 — 그래도 결과에 안 나와야 함
                        + "('2026-08-03', '18:00:00', 9000, '카페', 2, 103, 1, now(), now()), "
                        + "('2026-08-04', '18:00:00', 9000, '카페', 2, 103, 1, now(), now()), "
                        + "('2026-08-06', '18:00:00', 9000, '카페', 2, 103, 1, now(), now()), "
                        + "('2026-08-07', '18:00:00', 9000, '카페', 2, 103, 1, now(), now()), "
                        + "('2026-08-08', '18:00:00', 9000, '카페', 2, 103, 1, now(), now()), "
                        + "('2026-08-01', '09:00:00', 4000, '카페', 1, 105, 1, now(), now())");
    }

    @Test
    void 반경_안_장소만_인기순으로_상위_2개까지_반환하고_반경_밖은_제외한다() {
        // when
        RecommendationInfo info = recommendationQueryService.getNearbyRecommendations(1L, 37.5665, 126.9780, 1000);

        // then — 101(5회), 102(4회)가 1·2등이라 이 둘만 myTownPlaces에 남고, 104(3회)는 3등이라 잘림
        assertThat(info.myTownPlaces())
                .extracting(RecommendedPlaceInfo::placeId)
                .containsExactly(101L, 102L)
                .doesNotContain(103L, 104L);
        assertThat(info.myTownPlaces())
                .extracting(RecommendedPlaceInfo::dongName)
                .containsOnly("태평로");
    }

    @Test
    void 유저_최다방문_카테고리로_필터링하되_myTownPlaces와_겹치는_장소는_제외한다() {
        // when
        RecommendationInfo info = recommendationQueryService.getNearbyRecommendations(1L, 37.5665, 126.9780, 1000);

        // then — 유저1의 최근 30일 최다방문 카테고리는 카페(101:5 + 104:3 = 8 > 음식점 102:4)
        // 카페 중 1등인 101은 이미 myTownPlaces에 있으므로 제외되고, 다음 순위인 104만 남아야 함
        assertThat(info.sameCategoryPlaces())
                .extracting(RecommendedPlaceInfo::placeId, RecommendedPlaceInfo::category)
                .containsExactly(tuple(104L, "카페"));
    }

    @Test
    void 좋아요한_장소는_liked가_true다() {
        // given
        jdbcTemplate.update("DELETE FROM place_likes");
        jdbcTemplate.update(
                "INSERT INTO place_likes (user_id, place_id, created_at, updated_at) VALUES (1, 102, now(), now())");

        // when
        RecommendationInfo info = recommendationQueryService.getNearbyRecommendations(1L, 37.5665, 126.9780, 1000);

        // then
        boolean liked102 = info.myTownPlaces().stream()
                .filter(place -> place.placeId().equals(102L))
                .findFirst().orElseThrow()
                .liked();
        assertThat(liked102).isTrue();
    }

    @Test
    void 본인이_방문한_장소는_추천_후보에서_제외한다() {
        jdbcTemplate.update(
                "INSERT INTO consumptions (purchase_date, purchase_time, amount, category, user_id, place_id, sticker_item_id, created_at, updated_at) VALUES "
                        + "('2026-08-05', '13:00:00', 12000, '음식점', 1, 102, 1, now(), now())");

        // when
        RecommendationInfo info = recommendationQueryService.getNearbyRecommendations(1L, 37.5665, 126.9780, 1000);

        // then — 102는 방문했으므로 어떤 결과에도 나오지 않고, 다음 순위인 104가 myTownPlaces로 대신 올라옴
        assertThat(info.myTownPlaces())
                .extracting(RecommendedPlaceInfo::placeId)
                .containsExactly(101L, 104L)
                .doesNotContain(102L);
        assertThat(info.sameCategoryPlaces())
                .extracting(RecommendedPlaceInfo::placeId)
                .doesNotContain(102L);
    }

    @Test
    void 유효하지_않은_좌표는_예외를_던진다() {
        assertThatThrownBy(() -> recommendationQueryService.getNearbyRecommendations(1L, 91.0, 126.9780, 1000))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(RecommendationErrorCode.INVALID_COORDINATE);

        assertThatThrownBy(() -> recommendationQueryService.getNearbyRecommendations(1L, 37.5665, -181.0, 1000))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(RecommendationErrorCode.INVALID_COORDINATE);
    }

    @Test
    void 유효하지_않은_반경은_예외를_던진다() {
        assertThatThrownBy(() -> recommendationQueryService.getNearbyRecommendations(1L, 37.5665, 126.9780, -100))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(RecommendationErrorCode.INVALID_RADIUS);

        assertThatThrownBy(() -> recommendationQueryService.getNearbyRecommendations(1L, 37.5665, 126.9780, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(RecommendationErrorCode.INVALID_RADIUS);

        assertThatThrownBy(() -> recommendationQueryService.getNearbyRecommendations(1L, 37.5665, 126.9780, 50_001))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(RecommendationErrorCode.INVALID_RADIUS);
    }

    @Test
    void 반경_안에_아무_장소도_없으면_빈_결과를_반환한다() {
        // when — 완전히 무관한 좌표(북극 근처)
        RecommendationInfo info = recommendationQueryService.getNearbyRecommendations(1L, 80.0, 0.0, 1000);

        // then
        assertThat(info.myTownPlaces()).isEmpty();
        assertThat(info.sameCategoryPlaces()).isEmpty();
    }
}
