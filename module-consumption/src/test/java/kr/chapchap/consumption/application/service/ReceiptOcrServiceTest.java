package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ReceiptOcrCommand;
import kr.chapchap.consumption.application.info.ReceiptOcrInfo;
import kr.chapchap.consumption.application.port.ReceiptImageStorage;
import kr.chapchap.consumption.application.port.ReceiptOcrPort;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.place.application.info.GooglePlaceSearchResultInfo;
import kr.chapchap.place.application.service.GooglePlaceSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ReceiptOcrServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long RECEIPT_IMAGE_ID = 15L;
    private static final String OBJECT_KEY = "receipts/1/receipt-key";
    private static final byte[] IMAGE_CONTENT = new byte[]{1, 2, 3};
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 8, 17, 9, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-16T09:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private ReceiptImageValidator receiptImageValidator;

    @Mock
    private ReceiptOcrPort receiptOcrPort;

    @Mock
    private ReceiptOcrParser receiptOcrParser;

    @Mock
    private ReceiptImageStorage receiptImageStorage;

    @Mock
    private ReceiptImageCommandService receiptImageCommandService;

    @Mock
    private GooglePlaceSearchService googlePlaceSearchService;

    private ReceiptOcrService receiptOcrService;

    @BeforeEach
    void setUp() {
        receiptOcrService = new ReceiptOcrService(
                receiptImageValidator,
                receiptOcrPort,
                receiptOcrParser,
                receiptImageStorage,
                receiptImageCommandService,
                googlePlaceSearchService,
                FIXED_CLOCK
        );
    }

    @Test
    void OCR_인식_결과를_반환하고_영수증_이미지를_임시_저장한다() {
        // given
        List<String> lines = List.of("투썸플레이스 신논현점");
        ReceiptOcrParser.ParsedReceipt parsedReceipt = new ReceiptOcrParser.ParsedReceipt(
                "투썸플레이스 신논현점",
                "서울특별시 강남구 봉은사로 125 1층",
                LocalDate.of(2026, 7, 25),
                LocalTime.of(11, 20),
                33_000L
        );
        given(receiptImageValidator.validateAndGetContentType(IMAGE_CONTENT))
                .willReturn("image/png");
        given(receiptOcrPort.recognize(IMAGE_CONTENT, "image/png")).willReturn(lines);
        given(receiptOcrParser.parse(lines)).willReturn(parsedReceipt);
        given(receiptImageStorage.store(USER_ID, IMAGE_CONTENT, "image/png"))
                .willReturn(OBJECT_KEY);
        ReceiptImage receiptImage = ReceiptImage.createTemporary(
                USER_ID,
                OBJECT_KEY,
                "image/png",
                IMAGE_CONTENT.length,
                EXPIRES_AT
        );
        ReflectionTestUtils.setField(receiptImage, "id", RECEIPT_IMAGE_ID);
        given(receiptImageCommandService.saveTemporary(
                USER_ID,
                OBJECT_KEY,
                "image/png",
                IMAGE_CONTENT.length,
                EXPIRES_AT
        )).willReturn(receiptImage);
        GooglePlaceSearchResultInfo googlePlaceSearchResult = new GooglePlaceSearchResultInfo(
                "ChIJ123",
                "Google 검색 결과 장소",
                "서울특별시 강남구 테헤란로 1",
                37.5065,
                127.0241,
                "https://lh3.googleusercontent.com/photo"
        );
        given(googlePlaceSearchService.search(
                parsedReceipt.storeName(),
                parsedReceipt.address()
        )).willReturn(Optional.of(googlePlaceSearchResult));

        // when
        ReceiptOcrInfo result = receiptOcrService.recognize(
                new ReceiptOcrCommand(USER_ID, IMAGE_CONTENT)
        );

        // then
        assertThat(result.receiptImageId()).isEqualTo(RECEIPT_IMAGE_ID);
        assertThat(result.storeName()).isEqualTo("투썸플레이스 신논현점");
        assertThat(result.address()).isEqualTo("서울특별시 강남구 봉은사로 125 1층");
        assertThat(result.purchaseDate()).isEqualTo(LocalDate.of(2026, 7, 25));
        assertThat(result.purchaseTime()).isEqualTo(LocalTime.of(11, 20));
        assertThat(result.amount()).isEqualTo(33_000L);
        assertThat(result.googlePlaceSearchResult()).isEqualTo(googlePlaceSearchResult);

        InOrder inOrder = inOrder(
                receiptImageValidator,
                receiptOcrPort,
                receiptOcrParser,
                receiptImageStorage,
                receiptImageCommandService,
                googlePlaceSearchService
        );
        inOrder.verify(receiptImageValidator).validateAndGetContentType(IMAGE_CONTENT);
        inOrder.verify(receiptOcrPort).recognize(IMAGE_CONTENT, "image/png");
        inOrder.verify(receiptOcrParser).parse(lines);
        inOrder.verify(receiptImageStorage).store(USER_ID, IMAGE_CONTENT, "image/png");
        inOrder.verify(receiptImageCommandService).saveTemporary(
                USER_ID,
                OBJECT_KEY,
                "image/png",
                IMAGE_CONTENT.length,
                EXPIRES_AT
        );
        inOrder.verify(googlePlaceSearchService).search(
                parsedReceipt.storeName(),
                parsedReceipt.address()
        );
    }

    @Test
    void Google_Place_검색_결과가_없어도_OCR_인식_결과를_반환한다() {
        // given
        List<String> lines = List.of("찹찹카페");
        ReceiptOcrParser.ParsedReceipt parsedReceipt = new ReceiptOcrParser.ParsedReceipt(
                "찹찹카페",
                "서울특별시 강남구 테헤란로 123",
                LocalDate.of(2026, 8, 26),
                LocalTime.of(12, 30),
                15_000L
        );
        given(receiptImageValidator.validateAndGetContentType(IMAGE_CONTENT))
                .willReturn("image/png");
        given(receiptOcrPort.recognize(IMAGE_CONTENT, "image/png")).willReturn(lines);
        given(receiptOcrParser.parse(lines)).willReturn(parsedReceipt);
        given(receiptImageStorage.store(USER_ID, IMAGE_CONTENT, "image/png"))
                .willReturn(OBJECT_KEY);
        ReceiptImage receiptImage = ReceiptImage.createTemporary(
                USER_ID,
                OBJECT_KEY,
                "image/png",
                IMAGE_CONTENT.length,
                EXPIRES_AT
        );
        ReflectionTestUtils.setField(receiptImage, "id", RECEIPT_IMAGE_ID);
        given(receiptImageCommandService.saveTemporary(
                USER_ID,
                OBJECT_KEY,
                "image/png",
                IMAGE_CONTENT.length,
                EXPIRES_AT
        )).willReturn(receiptImage);
        given(googlePlaceSearchService.search(
                parsedReceipt.storeName(),
                parsedReceipt.address()
        )).willReturn(Optional.empty());

        // when
        ReceiptOcrInfo result = receiptOcrService.recognize(
                new ReceiptOcrCommand(USER_ID, IMAGE_CONTENT)
        );

        // then
        assertThat(result.storeName()).isEqualTo(parsedReceipt.storeName());
        assertThat(result.address()).isEqualTo(parsedReceipt.address());
        assertThat(result.purchaseDate()).isEqualTo(parsedReceipt.purchaseDate());
        assertThat(result.purchaseTime()).isEqualTo(parsedReceipt.purchaseTime());
        assertThat(result.amount()).isEqualTo(parsedReceipt.amount());
        assertThat(result.googlePlaceSearchResult()).isNull();
    }

    @Test
    void OCR_인식에_실패하면_영수증_이미지와_이미지_정보를_저장하지_않는다() {
        // given
        given(receiptImageValidator.validateAndGetContentType(IMAGE_CONTENT))
                .willReturn("image/png");
        given(receiptOcrPort.recognize(IMAGE_CONTENT, "image/png"))
                .willThrow(new BusinessException(
                        ConsumptionErrorCode.RECEIPT_OCR_RECOGNITION_FAILED
                ));

        // when & then
        assertThatThrownBy(() -> receiptOcrService.recognize(
                new ReceiptOcrCommand(USER_ID, IMAGE_CONTENT)
        )).isInstanceOf(BusinessException.class);

        then(receiptOcrParser).shouldHaveNoInteractions();
        then(receiptImageStorage).shouldHaveNoInteractions();
        then(receiptImageCommandService).shouldHaveNoInteractions();
        then(googlePlaceSearchService).shouldHaveNoInteractions();
    }

    @Test
    void 영수증_이미지_업로드에_실패하면_이미지_정보를_DB에_저장하지_않는다() {
        // given
        List<String> lines = List.of();
        given(receiptImageValidator.validateAndGetContentType(IMAGE_CONTENT))
                .willReturn("image/png");
        given(receiptOcrPort.recognize(IMAGE_CONTENT, "image/png")).willReturn(lines);
        given(receiptOcrParser.parse(lines)).willReturn(
                new ReceiptOcrParser.ParsedReceipt(null, null, null, null, null)
        );
        given(receiptImageStorage.store(USER_ID, IMAGE_CONTENT, "image/png"))
                .willThrow(new IllegalStateException("storage failed"));

        // when & then
        assertThatThrownBy(() -> receiptOcrService.recognize(
                new ReceiptOcrCommand(USER_ID, IMAGE_CONTENT)
        )).isInstanceOf(IllegalStateException.class);

        then(receiptImageCommandService).shouldHaveNoInteractions();
        then(googlePlaceSearchService).shouldHaveNoInteractions();
    }
}
