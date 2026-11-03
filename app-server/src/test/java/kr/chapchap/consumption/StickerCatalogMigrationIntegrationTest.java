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
    void 잘못된_스티커_이름을_수정하면서_ID와_기존_기록을_보존한다() {
        // given
        migrate(false);
        insertUserAndPlace();
        Map<String, Map<String, String>> corrections = Map.of(
                "운동", Map.of("운동화", "수영"),
                "미용/뷰티", Map.of("이발소", "사우나", "헤어커트", "헤어"),
                "편의점/마트", Map.of("물", "휴지"),
                "쇼핑", Map.of("하이힐", "운동화"),
                "기타", Map.of("열쇠", "위치핀", "우체국", "병원")
        );
        Map<Long, String> expectedNames = new LinkedHashMap<>();
        Map<Long, Long> recordStickers = new LinkedHashMap<>();
        corrections.forEach((category, names) -> names.forEach((oldName, newName) -> {
            Long stickerId = findStickerId(category, oldName);
            expectedNames.put(stickerId, newName);
            recordStickers.put(insertConsumption(category, stickerId), stickerId);
        }));

        // when
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("23").load().migrate();

        // then
        assertCatalog();
        expectedNames.forEach((id, name) -> assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM sticker_item WHERE id = ?", String.class, id
        )).isEqualTo(name));
        recordStickers.forEach((recordId, stickerId) -> assertThat(jdbcTemplate.queryForObject(
                "SELECT sticker_item_id FROM consumptions WHERE id = ?", Long.class, recordId
        )).isEqualTo(stickerId));
        assertThat(findStickerId("편의점/마트", "우유")).isNotNull();
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

    @Test
    void 프론트_64종과_이름을_맞추면서_모든_스티커_ID와_기존_기록을_보존한다() {
        // given
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("28").load().migrate();
        insertUserAndPlace();
        Map<String, Map<String, String>> corrections = Map.of(
                "카페", Map.of("말차", "녹차"),
                "음식점", Map.of("국수", "라면", "파스타", "스파게티", "식기", "접시", "파에야", "빠에야"),
                "편의점/마트", Map.of("쇼핑카트", "장바구니", "장바구니", "세탁", "음료", "사과 주스"),
                "취미/놀거리", Map.of("게임", "게임기"),
                "쇼핑", Map.of("원피스", "드레스", "안경", "선글라스"),
                "운동", Map.of("요가", "명상", "축구", "축구공", "테니스", "테니스공"),
                "미용/뷰티", Map.of("헤어", "헤어컷", "네일", "매니큐어"),
                "기타", Map.of("위치핀", "압정")
        );
        var originalStickers = jdbcTemplate.queryForList("SELECT id, category, name FROM sticker_item");
        Map<Long, Long> recordStickers = new LinkedHashMap<>();
        originalStickers.forEach(sticker -> {
            Long stickerId = ((Number) sticker.get("id")).longValue();
            recordStickers.put(insertConsumption((String) sticker.get("category"), stickerId), stickerId);
        });

        // when
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target("29").load().migrate();

        // then
        assertFrontendCatalog();
        originalStickers.forEach(sticker -> {
            String category = (String) sticker.get("category");
            String oldName = (String) sticker.get("name");
            String expectedName = corrections.get(category).getOrDefault(oldName, oldName);
            assertThat(findStickerId(category, expectedName)).isEqualTo(sticker.get("id"));
        });
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consumptions", Long.class)).isEqualTo(64L);
        recordStickers.forEach((recordId, stickerId) -> assertThat(jdbcTemplate.queryForMap(
                "SELECT sticker_item_id, amount, memo FROM consumptions WHERE id = ?", recordId
        )).containsEntry("sticker_item_id", stickerId)
                .containsEntry("amount", 5000L)
                .containsEntry("memo", "기존 기록"));
    }

    @Test
    void 이름_정정_후_로컬_시드에도_편의점_기록과_프론트_64종이_유지된다() {
        // given
        String[] locations = {"classpath:db/migration", "classpath:db/dev-data"};

        // when
        Flyway.configure().dataSource(dataSource).locations(locations).target("29").load().migrate();

        // then
        assertFrontendCatalog();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consumptions WHERE category = '편의점/마트'", Long.class
        )).isPositive();
        assertThat(jdbcTemplate.queryForList("""
                SELECT DISTINCT s.name FROM consumptions c
                JOIN sticker_item s ON c.sticker_item_id = s.id
                WHERE c.category = '편의점/마트'
                """, String.class)).containsExactly("장바구니");
    }

    private void assertFrontendCatalog() {
        assertCatalog();
        // 프론트 catalog.ts (blob 2cd7561079db3a2b9356bf6aab021cde6cb9c5c1)의 카테고리·이름 계약.
        Map<String, String> expectedNamesByCategory = Map.of(
                "카페", "커피|케이크|크루아상|아이스크림|컵케이크|쿠키|버블티|녹차",
                "음식점", "햄버거|빠에야|피자|접시|라면|밥|스파게티|초밥",
                "편의점/마트", "사과 주스|사과|통조림|편의점|세탁|우유|장바구니|휴지",
                "취미/놀거리", "볼링|카메라|영화|관람차|게임기|헤드폰|마이크|팔레트",
                "쇼핑", "드레스|선물|핸드백|청바지|반지|쇼핑백|운동화|선글라스",
                "운동", "자전거|명상|근육|달리기|축구공|수영|테니스공|역도",
                "미용/뷰티", "헤어컷|립스틱|로션|마사지|거울|매니큐어|사우나|반짝임",
                "기타", "책|알약|자동차|고양이|신용카드|강아지|병원|압정"
        );
        expectedNamesByCategory.forEach((category, names) -> assertThat(jdbcTemplate.queryForList(
                "SELECT name FROM sticker_item WHERE category = ?", String.class, category
        )).containsExactlyInAnyOrder(names.split("\\|")));
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
