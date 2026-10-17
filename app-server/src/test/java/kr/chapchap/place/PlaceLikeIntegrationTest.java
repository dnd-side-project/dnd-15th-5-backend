package kr.chapchap.place;

import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.place.application.command.LikedPlaceSearchCommand;
import kr.chapchap.place.application.info.LikedPlaceScrollInfo;
import kr.chapchap.place.application.info.PlacePhotoInfo;
import kr.chapchap.place.application.service.PlaceLikeCommandService;
import kr.chapchap.place.application.service.PlaceLikeQueryService;
import kr.chapchap.place.application.service.PlacePhotoService;
import kr.chapchap.recommendation.application.info.RecommendedPlaceInfo;
import kr.chapchap.recommendation.application.service.RecommendationQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PlaceLikeIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private PlaceLikeQueryService query;
    @Autowired private PlaceLikeCommandService commands;
    @Autowired private RecommendationQueryService recommendations;
    @MockitoBean private PlacePhotoService photos;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> placeIds = new ArrayList<>();
    private Long userId;
    private Long placeId;
    private Long visitedPlaceId;
    private Long distantPlaceId;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < 4; i++) {
            userIds.add(jdbc.queryForObject(
                    "INSERT INTO users (nickname, status) VALUES ('좋아요테스트', 'ACTIVE') RETURNING id", Long.class
            ));
        }
        userId = userIds.getFirst();
        placeId = createPlace(126.9780, 37.5670);
        visitedPlaceId = createPlace(126.9790, 37.5660);
        distantPlaceId = createPlace(129.0756, 35.1796);
        for (int i = 0; i < 3; i++) {
            record(userIds.get(1), placeId, "카페");
        }
        record(userIds.get(2), placeId, "쇼핑");
        record(userIds.get(3), placeId, "쇼핑");
        record(userId, visitedPlaceId, "취미/놀거리");
    }

    @AfterEach
    void cleanUp() {
        for (Long id : userIds) {
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
        for (Long id : placeIds) {
            jdbc.update("DELETE FROM places WHERE id = ?", id);
        }
    }

    @Test
    void 추천에서_좋아요하면_기록수_대표카테고리와_사진을_목록에서_조회한다() throws Exception {
        // given
        given(photos.findPhotos(anyMap())).willAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return Map.of(placeId, List.of(new PlacePhotoInfo("photo-url", "maps-url")));
        });

        // when
        mvc.perform(put("/v1/places/{placeId}/likes", placeId).with(asUser(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));

        // then
        mvc.perform(get("/v1/places/likes").with(asUser(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places.length()").value(1))
                .andExpect(jsonPath("$.data.places[0].placeId").value(placeId))
                .andExpect(jsonPath("$.data.places[0].name").value("테스트 가게"))
                .andExpect(jsonPath("$.data.places[0].category").value("카페"))
                .andExpect(jsonPath("$.data.places[0].address").value("테스트 주소"))
                .andExpect(jsonPath("$.data.places[0].photos[0].thumbnailUrl").value("photo-url"))
                .andExpect(jsonPath("$.data.places[0].photos[0].googleMapsUri").value("maps-url"))
                .andExpect(jsonPath("$.data.categories[0]").value("카페"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
        assertThat(recommendations.getNearbyRecommendations(userId, 37.5665, 126.9780, 1000).myTownPlaces())
                .extracting(RecommendedPlaceInfo::placeId, RecommendedPlaceInfo::category,
                        RecommendedPlaceInfo::visitCount, RecommendedPlaceInfo::liked)
                .containsExactly(tuple(placeId, "카페", 5L, true));
    }

    @Test
    void 기록이_바뀌면_추천과_좋아요의_대표카테고리와_필터가_함께_바뀐다() {
        // given
        commands.toggle(userId, placeId);
        assertThat(search("카페", null, 5).places()).hasSize(1);

        // when
        jdbc.update("DELETE FROM consumptions WHERE place_id = ? AND category = '카페'", placeId);

        // then
        LikedPlaceScrollInfo oldCategory = search("카페", null, 5);
        assertThat(oldCategory.places()).isEmpty();
        assertThat(oldCategory.categories()).containsExactly("쇼핑");
        assertThat(search("쇼핑", null, 5).places()).singleElement()
                .extracting(LikedPlaceScrollInfo.LikedPlaceInfo::category).isEqualTo("쇼핑");
        assertThat(recommendations.getNearbyRecommendations(userId, 37.5665, 126.9780, 1000).myTownPlaces())
                .extracting(RecommendedPlaceInfo::placeId, RecommendedPlaceInfo::category)
                .containsExactly(tuple(placeId, "쇼핑"));
    }

    @Test
    void 기록이_없어져도_좋아요는_남고_이미_방문했거나_먼_장소도_조회한다() {
        // given
        commands.toggle(userId, placeId);
        commands.toggle(userId, visitedPlaceId);
        commands.toggle(userId, distantPlaceId);

        // when
        jdbc.update("DELETE FROM consumptions WHERE place_id = ?", placeId);
        LikedPlaceScrollInfo result = search(null, null, 5);

        // then
        assertThat(result.places()).extracting(LikedPlaceScrollInfo.LikedPlaceInfo::placeId,
                        LikedPlaceScrollInfo.LikedPlaceInfo::category)
                .containsExactly(tuple(distantPlaceId, null), tuple(visitedPlaceId, "취미/놀거리"), tuple(placeId, null));
        assertThat(result.categories()).containsExactly("취미/놀거리");
        assertThat(result.places()).allSatisfy(place -> assertThat(place.photos()).isEmpty());
    }

    @Test
    void 최근_좋아요순_커서로_조회하고_해제된_커서의_다음_페이지도_조회한다() {
        // given
        commands.toggle(userId, placeId);
        commands.toggle(userId, visitedPlaceId);
        commands.toggle(userId, distantPlaceId);
        LikedPlaceScrollInfo first = search(null, null, 1);
        assertThat(first.places()).singleElement()
                .extracting(LikedPlaceScrollInfo.LikedPlaceInfo::placeId).isEqualTo(distantPlaceId);
        assertThat(first.hasNext()).isTrue();
        assertThat(first.categories()).containsExactly("취미/놀거리", "카페");

        // when
        commands.toggle(userId, distantPlaceId);
        LikedPlaceScrollInfo next = search(null, first.nextCursorId(), 5);

        // then
        assertThat(next.places()).extracting(LikedPlaceScrollInfo.LikedPlaceInfo::placeId)
                .containsExactly(visitedPlaceId, placeId);
        assertThat(next.hasNext()).isFalse();
        assertThat(next.nextCursorId()).isNull();
    }

    @Test
    void 기존_토글로_해제하면_목록에서_빠지고_다른_사용자의_좋아요는_유지한다() throws Exception {
        // given
        Long otherUserId = userIds.get(1);
        commands.toggle(userId, placeId);
        commands.toggle(otherUserId, placeId);

        // when
        mvc.perform(put("/v1/places/{placeId}/likes", placeId).with(asUser(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false));

        // then
        assertThat(search(null, null, 5).places()).isEmpty();
        assertThat(query.findLikedPlaces(new LikedPlaceSearchCommand(otherUserId, null, null, 5)).places())
                .singleElement().extracting(LikedPlaceScrollInfo.LikedPlaceInfo::placeId).isEqualTo(placeId);
    }

    @Test
    void 인증과_페이지_크기와_커서를_검증한다() throws Exception {
        // given & when & then
        mvc.perform(get("/v1/places/likes")).andExpect(status().isUnauthorized());
        mvc.perform(put("/v1/places/{placeId}/likes", placeId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/places/likes").with(jwt().jwt(token -> token.subject(userId.toString()))
                        .authorities(new SimpleGrantedAuthority("SCOPE_signup"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/places/likes").param("size", "6").with(asUser(userId)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/places/likes").param("cursorId", "0").with(asUser(userId)))
                .andExpect(status().isBadRequest());
    }

    private LikedPlaceScrollInfo search(String category, Long cursorId, int size) {
        return query.findLikedPlaces(new LikedPlaceSearchCommand(userId, category, cursorId, size));
    }

    private RequestPostProcessor asUser(Long id) {
        return jwt().jwt(token -> token.subject(id.toString()))
                .authorities(new SimpleGrantedAuthority("SCOPE_user"));
    }

    private Long createPlace(double longitude, double latitude) {
        Long id = jdbc.queryForObject("""
                INSERT INTO places (google_place_id, name, road_address, administrative_dong_code,
                    administrative_dong_name, location)
                VALUES (?, '테스트 가게', '테스트 주소', '1100000', '태평로',
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) RETURNING id
                """, Long.class, UUID.randomUUID().toString(), longitude, latitude);
        placeIds.add(id);
        return id;
    }

    private void record(Long authorId, Long targetPlaceId, String category) {
        jdbc.update("""
                INSERT INTO consumptions (purchase_date, purchase_time, amount, category,
                    user_id, place_id, sticker_item_id)
                VALUES (CURRENT_DATE, '12:00:00', 5000, ?, ?, ?,
                    (SELECT id FROM sticker_item WHERE category = '카페' AND name = '커피'))
                """, category, authorId, targetPlaceId);
    }
}
