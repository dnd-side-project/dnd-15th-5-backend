package kr.chapchap.consumption;

import kr.chapchap.account.domain.entity.User;
import kr.chapchap.account.domain.repository.UserRepository;
import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.command.PlaceResolveCommand;
import kr.chapchap.consumption.application.info.ConsumptionInfo;
import kr.chapchap.consumption.application.port.PlaceResolvePort;
import kr.chapchap.consumption.application.service.ConsumptionCreateService;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.domain.repository.ReceiptImageRepository;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConsumptionCreatePersistenceIntegrationTest {

    private final ConsumptionCreateService consumptionCreateService;
    private final ReceiptImageRepository receiptImageRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PlaceResolvePort placeResolvePort;

    @Autowired
    ConsumptionCreatePersistenceIntegrationTest(
            ConsumptionCreateService consumptionCreateService,
            ReceiptImageRepository receiptImageRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.consumptionCreateService = consumptionCreateService;
        this.receiptImageRepository = receiptImageRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM receipt_images");
        jdbcTemplate.update("DELETE FROM consumptions");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void 장소_확인은_트랜잭션_밖에서_실행하고_소비와_영수증과_스티커를_함께_저장한다() {
        // given
        User user = saveActiveUser("찹찹이");
        Long placeId = insertPlace("ChIJ-integration-place");
        ReceiptImage receiptImage = receiptImageRepository.save(ReceiptImage.createTemporary(
                user.getId(),
                "receipts/1/receipt-key",
                "image/png",
                100L,
                LocalDateTime.now().plusHours(1)
        ));
        given(placeResolvePort.resolve(any(PlaceResolveCommand.class)))
                .willAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    return placeId;
                });

        // when
        ConsumptionInfo result = consumptionCreateService.create(command(
                user.getId(),
                receiptImage.getId(),
                "카페"
        ));

        // then
        Map<String, Object> consumption = jdbcTemplate.queryForMap(
                "SELECT * FROM consumptions WHERE id = ?",
                result.id()
        );
        assertThat(consumption.get("user_id")).isEqualTo(user.getId());
        assertThat(consumption.get("place_id")).isEqualTo(placeId);
        assertThat(consumption.get("amount")).isEqualTo(33_000L);
        assertThat(consumption.get("category")).isEqualTo("카페");
        assertThat(consumption.get("purchase_date")).hasToString("2026-07-25");
        assertThat(consumption.get("purchase_time")).hasToString("11:20:00");
        assertThat(consumption.get("sticker_item_id")).isNotNull();

        String stickerCategory = jdbcTemplate.queryForObject(
                "SELECT category FROM sticker_item WHERE id = ?",
                String.class,
                consumption.get("sticker_item_id")
        );
        assertThat(stickerCategory).isEqualTo("카페");

        Map<String, Object> attachedReceipt = jdbcTemplate.queryForMap(
                "SELECT * FROM receipt_images WHERE id = ?",
                receiptImage.getId()
        );
        assertThat(attachedReceipt.get("status")).isEqualTo("ATTACHED");
        assertThat(attachedReceipt.get("consumption_id")).isEqualTo(result.id());
        assertThat(attachedReceipt.get("attached_at")).isNotNull();
        assertThat(attachedReceipt.get("expires_at")).isNull();
    }

    @Test
    void 영수증과_스티커가_없는_카테고리도_소비_기록을_저장한다() {
        // given
        User user = saveActiveUser("냠냠이");
        Long placeId = insertPlace("ChIJ-manual-place");
        given(placeResolvePort.resolve(any(PlaceResolveCommand.class))).willReturn(placeId);

        // when
        ConsumptionInfo result = consumptionCreateService.create(command(
                user.getId(),
                null,
                "테스트-스티커-미등록"
        ));

        // then
        Map<String, Object> consumption = jdbcTemplate.queryForMap(
                "SELECT * FROM consumptions WHERE id = ?",
                result.id()
        );
        assertThat(consumption.get("sticker_item_id")).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM receipt_images",
                Long.class
        )).isZero();
    }

    @Test
    void 다른_사용자의_영수증이면_소비_기록을_저장하지_않는다() {
        // given
        User user = saveActiveUser("소비자");
        User otherUser = saveActiveUser("영수증주인");
        Long placeId = insertPlace("ChIJ-owned-receipt-place");
        ReceiptImage receiptImage = receiptImageRepository.save(ReceiptImage.createTemporary(
                otherUser.getId(),
                "receipts/2/receipt-key",
                "image/png",
                100L,
                LocalDateTime.now().plusHours(1)
        ));
        given(placeResolvePort.resolve(any(PlaceResolveCommand.class))).willReturn(placeId);

        // when & then
        assertThatThrownBy(() -> consumptionCreateService.create(command(
                user.getId(),
                receiptImage.getId(),
                "카페"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ConsumptionErrorCode.RECEIPT_IMAGE_NOT_FOUND);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consumptions",
                Long.class
        )).isZero();
    }

    private ConsumptionCreateCommand command(Long userId, Long receiptImageId, String category) {
        return new ConsumptionCreateCommand(
                userId,
                receiptImageId,
                new PlaceResolveCommand(
                        "ChIJxxxxxxxxxxxxxxxx",
                        "투썸플레이스 신논현점",
                        "서울특별시 강남구 봉은사로 125 1층",
                        37.506481,
                        127.024551
                ),
                LocalDate.of(2026, 7, 25),
                LocalTime.of(11, 20),
                33_000L,
                category
        );
    }

    private User saveActiveUser(String nickname) {
        User user = User.create(nickname);
        user.completeTermsAgreement();
        return userRepository.save(user);
    }

    private Long insertPlace(String googlePlaceId) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO places (
                            google_place_id,
                            name,
                            road_address,
                            administrative_dong_code,
                            administrative_dong_name,
                            location
                        ) VALUES (?, ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography)
                        RETURNING id
                        """,
                Long.class,
                googlePlaceId,
                "투썸플레이스 신논현점",
                "서울특별시 강남구 봉은사로 125 1층",
                "11680650",
                "역삼1동",
                127.024551,
                37.506481
        );
    }
}
