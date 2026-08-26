package kr.chapchap.consumption;

import kr.chapchap.config.CorsConfig;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.config.WebMvcConfig;
import kr.chapchap.consumption.api.controller.ReceiptOcrController;
import kr.chapchap.consumption.application.command.ReceiptOcrCommand;
import kr.chapchap.consumption.application.info.ReceiptOcrInfo;
import kr.chapchap.consumption.application.service.ReceiptOcrService;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import kr.chapchap.place.application.info.GooglePlaceSearchResultInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        CorsConfig.class,
        WebMvcConfig.class,
        GlobalExceptionHandler.class
})
@WebMvcTest(ReceiptOcrController.class)
class ReceiptOcrApiTest {

    private static final Long USER_ID = 1L;
    private static final byte[] IMAGE_CONTENT = new byte[]{1, 2, 3};

    private final MockMvc mockMvc;

    @MockitoBean
    private ReceiptOcrService receiptOcrService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ReceiptOcrApiTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void user_scope로_영수증_OCR을_요청한다() throws Exception {
        // given
        MockMultipartFile receiptImage = receiptImage();
        given(receiptOcrService.recognize(any(ReceiptOcrCommand.class)))
                .willReturn(new ReceiptOcrInfo(
                        15L,
                        "투썸플레이스 신논현점",
                        "서울특별시 강남구 봉은사로 125 1층",
                        LocalDate.of(2026, 7, 25),
                        LocalTime.of(11, 20),
                        33_000L,
                        new GooglePlaceSearchResultInfo(
                                "ChIJ123",
                                "투썸플레이스 신논현점",
                                "서울특별시 강남구 봉은사로 125 1층",
                                37.5065,
                                127.0241,
                                "https://lh3.googleusercontent.com/photo"
                        )
                ));

        // when & then
        mockMvc.perform(multipart("/consumptions/receipt-ocr")
                        .file(receiptImage)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.receiptImageId").value(15L))
                .andExpect(jsonPath("$.data.storeName").value("투썸플레이스 신논현점"))
                .andExpect(jsonPath("$.data.address")
                        .value("서울특별시 강남구 봉은사로 125 1층"))
                .andExpect(jsonPath("$.data.purchaseDate").value("2026-07-25"))
                .andExpect(jsonPath("$.data.purchaseTime").value("11:20:00"))
                .andExpect(jsonPath("$.data.amount").value(33_000L))
                .andExpect(jsonPath("$.data.googlePlaceSearchResult.googlePlaceId")
                        .value("ChIJ123"))
                .andExpect(jsonPath("$.data.googlePlaceSearchResult.placeName")
                        .value("투썸플레이스 신논현점"))
                .andExpect(jsonPath("$.data.googlePlaceSearchResult.roadAddress")
                        .value("서울특별시 강남구 봉은사로 125 1층"))
                .andExpect(jsonPath("$.data.googlePlaceSearchResult.latitude").value(37.5065))
                .andExpect(jsonPath("$.data.googlePlaceSearchResult.longitude").value(127.0241))
                .andExpect(jsonPath("$.data.googlePlaceSearchResult.thumbnailUrl")
                        .value("https://lh3.googleusercontent.com/photo"))
                .andExpect(jsonPath("$.data.googlePlaceSearchResult.photoName").doesNotExist());

        ArgumentCaptor<ReceiptOcrCommand> commandCaptor =
                ArgumentCaptor.forClass(ReceiptOcrCommand.class);
        then(receiptOcrService).should().recognize(commandCaptor.capture());
        assertThat(commandCaptor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(commandCaptor.getValue().receiptImageContent()).isEqualTo(IMAGE_CONTENT);
    }

    @Test
    void 인식하지_못한_항목은_null로_반환한다() throws Exception {
        // given
        given(receiptOcrService.recognize(any(ReceiptOcrCommand.class)))
                .willReturn(new ReceiptOcrInfo(15L, null, null, null, null, null, null));

        // when & then
        mockMvc.perform(multipart("/consumptions/receipt-ocr")
                        .file(receiptImage())
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiptImageId").value(15L))
                .andExpect(jsonPath("$.data.storeName").value((Object) null))
                .andExpect(jsonPath("$.data.address").value((Object) null))
                .andExpect(jsonPath("$.data.purchaseDate").value((Object) null))
                .andExpect(jsonPath("$.data.purchaseTime").value((Object) null))
                .andExpect(jsonPath("$.data.amount").value((Object) null))
                .andExpect(jsonPath("$.data.googlePlaceSearchResult").value((Object) null));
    }

    @Test
    void Access_Token이_없으면_영수증_OCR을_요청할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(multipart("/consumptions/receipt-ocr").file(receiptImage()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        then(receiptOcrService).shouldHaveNoInteractions();
    }

    @Test
    void signup_scope로_영수증_OCR을_요청할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(multipart("/consumptions/receipt-ocr")
                        .file(receiptImage())
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_signup"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        then(receiptOcrService).shouldHaveNoInteractions();
    }

    @Test
    void 숫자가_아닌_JWT_subject면_인증_오류를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(multipart("/consumptions/receipt-ocr")
                        .file(receiptImage())
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("invalid-user-id"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C006"));

        then(receiptOcrService).shouldHaveNoInteractions();
    }

    @Test
    void 영수증_이미지가_누락되면_입력_오류를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(multipart("/consumptions/receipt-ocr")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.data.receiptImage")
                        .value("영수증 이미지는 필수입니다."));

        then(receiptOcrService).shouldHaveNoInteractions();
    }

    @Test
    void OCR_호출_대기_한도를_초과하면_요청_제한_오류를_반환한다() throws Exception {
        // given
        given(receiptOcrService.recognize(any(ReceiptOcrCommand.class)))
                .willThrow(new BusinessException(
                        ConsumptionErrorCode.RECEIPT_OCR_REQUEST_LIMIT_EXCEEDED
                ));

        // when & then
        mockMvc.perform(multipart("/consumptions/receipt-ocr")
                        .file(receiptImage())
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_user"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("CONSUMPTION009"))
                .andExpect(jsonPath("$.message")
                        .value("OCR 요청이 많습니다. 잠시 후 다시 시도해주세요."));
    }

    private MockMultipartFile receiptImage() {
        return new MockMultipartFile(
                "receiptImage",
                "receipt.png",
                MediaType.IMAGE_PNG_VALUE,
                IMAGE_CONTENT
        );
    }
}
