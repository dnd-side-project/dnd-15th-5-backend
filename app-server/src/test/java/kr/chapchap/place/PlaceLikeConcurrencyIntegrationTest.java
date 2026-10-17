package kr.chapchap.place;

import jakarta.persistence.EntityManager;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.place.domain.entity.PlaceLike;
import kr.chapchap.place.domain.repository.PlaceLikeRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PlaceLikeConcurrencyIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entityManager;
    @MockitoSpyBean private PlaceLikeRepository likes;

    private Long userId;
    private Long placeId;

    @BeforeEach
    void setUp() {
        userId = jdbc.queryForObject(
                "INSERT INTO users (nickname, status) VALUES ('동시성테스트', 'ACTIVE') RETURNING id", Long.class
        );
        placeId = jdbc.queryForObject("""
                INSERT INTO places (name, road_address, administrative_dong_code, administrative_dong_name, location)
                VALUES ('테스트 가게', '테스트 주소', '1100000', '태평로',
                    ST_SetSRID(ST_MakePoint(126.9780, 37.5670), 4326)::geography) RETURNING id
                """, Long.class);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
        jdbc.update("DELETE FROM places WHERE id = ?", placeId);
    }

    @Test
    void 동시에_미등록_상태를_읽고_등록하면_하나는_성공하고_하나는_409를_반환한다() throws Exception {
        // given
        synchronizeReads();

        // when
        List<MvcResult> results = toggleConcurrently();

        // then
        assertConflict(results);
        assertThat(likeCount()).isEqualTo(1);
    }

    @Test
    void 동시에_같은_좋아요를_읽고_해제하면_하나는_성공하고_하나는_409를_반환한다() throws Exception {
        // given
        jdbc.update("INSERT INTO place_likes (user_id, place_id) VALUES (?, ?)", userId, placeId);
        synchronizeReads();

        // when
        List<MvcResult> results = toggleConcurrently();

        // then
        assertConflict(results);
        assertThat(likeCount()).isZero();
    }

    private void synchronizeReads() {
        CyclicBarrier barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            // 실제 트랜잭션에서 엔티티를 읽되, 두 요청이 같은 상태를 읽을 때까지 쓰기를 지연한다.
            var result = entityManager.createQuery(
                            "select pl from PlaceLike pl where pl.userId = :userId and pl.placeId = :placeId",
                            PlaceLike.class
                    ).setParameter("userId", userId).setParameter("placeId", placeId)
                    .getResultList().stream().findFirst();
            barrier.await(10, TimeUnit.SECONDS);
            return result;
        }).when(likes).findByUserIdAndPlaceId(userId, placeId);
    }

    private List<MvcResult> toggleConcurrently() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(this::toggle);
            var second = executor.submit(this::toggle);
            return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
    }

    private MvcResult toggle() throws Exception {
        return mvc.perform(put("/v1/places/{placeId}/likes", placeId)
                        .with(jwt().jwt(token -> token.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andReturn();
    }

    private void assertConflict(List<MvcResult> results) throws Exception {
        assertThat(results).extracting(result -> result.getResponse().getStatus())
                .containsExactlyInAnyOrder(200, 409);
        MvcResult conflict = results.stream().filter(result -> result.getResponse().getStatus() == 409)
                .findFirst().orElseThrow();
        assertThat(conflict.getResponse().getContentAsString()).contains("\"code\":\"PLACE007\"");
    }

    private int likeCount() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM place_likes WHERE user_id = ? AND place_id = ?", Integer.class, userId, placeId
        );
    }
}
