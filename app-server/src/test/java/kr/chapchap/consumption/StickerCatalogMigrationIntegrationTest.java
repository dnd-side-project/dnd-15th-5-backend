package kr.chapchap.consumption;

import kr.chapchap.core.test.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.flyway.target=21")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StickerCatalogMigrationIntegrationTest {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Autowired
    StickerCatalogMigrationIntegrationTest(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Test
    void 기존_기록을_보존하면서_공통과_스페셜과_제외된_스티커를_카테고리별로_교체한다() {
        // given
        insertUserAndPlace();
        Map<String, String> representatives = Map.of(
                "카페", "커피", "음식점", "밥", "운동", "근육", "미용/뷰티", "립스틱",
                "편의점/마트", "쇼핑카트", "쇼핑", "쇼핑백", "취미/놀거리", "팔레트", "기타", "신용카드"
        );
        Map<Long, String> expectedNames = new LinkedHashMap<>();
        Long commonId = findStickerId("공통", "눈");
        Long specialId = findStickerId("스페셜", "왕관");
        for (Map.Entry<String, String> entry : representatives.entrySet()) {
            expectedNames.put(insertConsumption(entry.getKey(), commonId), entry.getValue());
            expectedNames.put(insertConsumption(entry.getKey(), specialId), entry.getValue());
        }
        expectedNames.put(insertConsumption("카페", findStickerId("카페", "도넛")), "커피");
        expectedNames.put(insertConsumption("취미/놀거리", findStickerId("취미/놀거리", "LP")), "팔레트");
        Long coffeeId = findStickerId("카페", "커피");
        Long retainedRecordId = insertConsumption("카페", coffeeId);
        Long imageId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO consumption_images (user_id, object_key, content_type, file_size_bytes, status)
                        VALUES (1, 'consumptions/1/photo', 'image/png', 100, 'ATTACHED') RETURNING id
                        """,
                Long.class
        );
        Long photoRecordId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO consumptions (
                            user_id, place_id, purchase_date, purchase_time, category, image_id, memo
                        ) VALUES (1, 1, '2026-10-10', '12:00:00', '카페', ?, '사진 기록') RETURNING id
                        """,
                Long.class,
                imageId
        );

        // when
        migrate(false);

        // then
        assertCatalog();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consumptions", Long.class)).isEqualTo(20L);
        expectedNames.forEach((id, name) -> {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    """
                            SELECT s.name, c.category = s.category AS category_matches, c.amount, c.memo
                            FROM consumptions c JOIN sticker_item s ON c.sticker_item_id = s.id
                            WHERE c.id = ?
                            """,
                    id
            );
            assertThat(row.get("name")).isEqualTo(name);
            assertThat(row.get("category_matches")).isEqualTo(true);
            assertThat(row.get("amount")).isEqualTo(5000L);
            assertThat(row.get("memo")).isEqualTo("기존 기록");
        });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT sticker_item_id FROM consumptions WHERE id = ?", Long.class, retainedRecordId
        )).isEqualTo(coffeeId);
        Map<String, Object> photo = jdbcTemplate.queryForMap(
                "SELECT image_id, sticker_item_id, amount, memo FROM consumptions WHERE id = ?", photoRecordId
        );
        assertThat(photo.get("image_id")).isEqualTo(imageId);
        assertThat(photo.get("sticker_item_id")).isNull();
        assertThat(photo.get("amount")).isNull();
        assertThat(photo.get("memo")).isEqualTo("사진 기록");
    }

    @Test
    void 알_수_없는_소비_카테고리는_임의로_바꾸지_않고_마이그레이션을_롤백한다() {
        // given
        insertUserAndPlace();
        Long oldStickerId = findStickerId("공통", "눈");
        Long consumptionId = insertConsumption("미정", oldStickerId);

        // when
        // then
        assertThatThrownBy(() -> migrate(false)).isInstanceOf(FlywayException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sticker_item", Long.class)).isEqualTo(12L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT sticker_item_id FROM consumptions WHERE id = ?", Long.class, consumptionId
        )).isEqualTo(oldStickerId);
    }

    @Test
    void 로컬_시드도_새_64개_목록을_유지하고_소비_카테고리에_맞는_스티커를_연결한다() {
        // given
        boolean includeLocalSeeds = true;

        // when
        migrate(includeLocalSeeds);

        // then
        assertCatalog();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consumptions", Long.class)).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM consumptions c
                        JOIN sticker_item s ON c.sticker_item_id = s.id
                        WHERE c.category <> s.category
                        """,
                Long.class
        )).isZero();
    }

    private void migrate(boolean includeLocalSeeds) {
        String[] locations = includeLocalSeeds
                ? new String[]{"classpath:db/migration", "classpath:db/dev-data"}
                : new String[]{"classpath:db/migration"};
        Flyway.configure().dataSource(dataSource).locations(locations).target("22").load().migrate();
    }

    private void assertCatalog() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sticker_item", Long.class)).isEqualTo(64L);
        assertThat(jdbcTemplate.queryForList(
                "SELECT COUNT(*) FROM sticker_item GROUP BY category", Long.class
        )).hasSize(8).containsOnly(8L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sticker_item WHERE category IN ('공통', '스페셜')", Long.class
        )).isZero();
    }

    private void insertUserAndPlace() {
        jdbcTemplate.update("INSERT INTO users (id, nickname, status) VALUES (1, '마이그레이션', 'ACTIVE')");
        jdbcTemplate.update("""
                INSERT INTO places (id, name, road_address, administrative_dong_code, administrative_dong_name, location)
                VALUES (1, '테스트 가게', '테스트 주소', '11680650', '역삼1동',
                    ST_SetSRID(ST_MakePoint(127.024551, 37.506481), 4326)::geography)
                """);
    }

    private Long findStickerId(String category, String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sticker_item WHERE category = ? AND name = ?", Long.class, category, name
        );
    }

    private Long insertConsumption(String category, Long stickerId) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO consumptions (
                            user_id, place_id, purchase_date, purchase_time, amount, category, sticker_item_id, memo
                        ) VALUES (1, 1, '2026-10-10', '12:00:00', 5000, ?, ?, '기존 기록') RETURNING id
                        """,
                Long.class,
                category, stickerId
        );
    }
}
