package kr.chapchap.consumption;

import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ConsumptionImageSchemaIntegrationTest {

    private final JdbcTemplate jdbcTemplate;
    private Long userId;
    private Long placeId;
    private Long stickerId;

    @Autowired
    ConsumptionImageSchemaIntegrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (nickname, status) VALUES ('사진테스트', 'ACTIVE') RETURNING id",
                Long.class
        );
        placeId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO places (
                            name, road_address, administrative_dong_code, administrative_dong_name, location
                        ) VALUES ('테스트 가게', '테스트 주소', '11680650', '역삼1동',
                            ST_SetSRID(ST_MakePoint(127.024551, 37.506481), 4326)::geography)
                        RETURNING id
                        """,
                Long.class
        );
        stickerId = jdbcTemplate.queryForObject(
                "SELECT id FROM sticker_item WHERE category = '카페' AND name = '커피'",
                Long.class
        );
    }

    @Test
    void 기존_방식으로_스티커와_금액을_저장할_수_있다() {
        // given
        Long amount = 5000L;

        // when
        Long id = insertConsumption(stickerId, null, amount, null);

        // then
        Map<String, Object> row = findConsumption(id);
        assertThat(row.get("sticker_item_id")).isEqualTo(stickerId);
        assertThat(row.get("amount")).isEqualTo(amount);
        assertThat(row.get("image_id")).isNull();
        assertThat(row.get("memo")).isNull();
    }

    @Test
    void 사진과_메모를_저장하고_금액을_생략할_수_있다() {
        // given
        Long imageId = insertImage();
        String memo = "첫 방문\n다음에도 와야지";

        // when
        Long id = insertConsumption(null, imageId, null, memo);

        // then
        Map<String, Object> row = findConsumption(id);
        assertThat(row.get("image_id")).isEqualTo(imageId);
        assertThat(row.get("memo")).isEqualTo(memo);
        assertThat(row.get("sticker_item_id")).isNull();
        assertThat(row.get("amount")).isNull();
    }

    @Test
    void 스티커와_사진을_동시에_연결할_수_없다() {
        // given
        Long imageId = insertImage();

        // when
        // then
        assertThatThrownBy(() -> insertConsumption(stickerId, imageId, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_consumptions_sticker_or_image");
    }

    @Test
    void 스티커와_사진을_모두_생략할_수_없다() {
        // given
        Long absentId = null;

        // when
        // then
        assertThatThrownBy(() -> insertConsumption(absentId, absentId, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_consumptions_sticker_or_image");
    }

    @Test
    void 같은_사진을_여러_기록에_연결할_수_없다() {
        // given
        Long imageId = insertImage();
        insertConsumption(null, imageId, null, null);

        // when
        // then
        assertThatThrownBy(() -> insertConsumption(null, imageId, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_consumptions_image_id");
    }

    @Test
    void 기록에_연결된_이미지_행은_바로_삭제할_수_없다() {
        // given
        Long imageId = insertImage();
        insertConsumption(null, imageId, null, null);

        // when
        // then
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM consumption_images WHERE id = ?", imageId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 회원_삭제시_소비기록과_임시_이미지와_연결된_이미지_행을_삭제한다() {
        // given
        Long imageId = insertImage();
        insertConsumption(null, imageId, null, null);
        jdbcTemplate.update(
                "UPDATE consumption_images SET status = 'ATTACHED', expires_at = NULL, attached_at = now() WHERE id = ?",
                imageId
        );
        insertImage();

        // when
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);

        // then
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consumption_images WHERE user_id = ?", Long.class, userId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consumptions WHERE user_id = ?", Long.class, userId
        )).isZero();
    }

    private Long insertImage() {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO consumption_images (
                            user_id, object_key, content_type, file_size_bytes, status, expires_at
                        ) VALUES (?, ?, 'image/png', 100, 'TEMPORARY', now() + interval '1 hour')
                        RETURNING id
                        """,
                Long.class,
                userId,
                "consumptions/" + userId + "/" + java.util.UUID.randomUUID()
        );
    }

    private Long insertConsumption(Long stickerItemId, Long imageId, Long amount, String memo) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO consumptions (
                            user_id, place_id, purchase_date, purchase_time, category,
                            sticker_item_id, image_id, amount, memo
                        ) VALUES (?, ?, '2026-10-10', '12:00:00', '카페', ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                userId, placeId, stickerItemId, imageId, amount, memo
        );
    }

    private Map<String, Object> findConsumption(Long id) {
        return jdbcTemplate.queryForMap("SELECT * FROM consumptions WHERE id = ?", id);
    }
}
