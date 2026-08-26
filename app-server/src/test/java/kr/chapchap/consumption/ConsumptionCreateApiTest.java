package kr.chapchap.consumption;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.chapchap.config.CorsConfig;
import kr.chapchap.config.SecurityConfig;
import kr.chapchap.config.WebMvcConfig;
import kr.chapchap.consumption.api.controller.ConsumptionCreateController;
import kr.chapchap.consumption.application.command.ConsumptionCreateCommand;
import kr.chapchap.consumption.application.info.ConsumptionCreateInfo;
import kr.chapchap.consumption.application.service.ConsumptionCreateService;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.web.exception.GlobalExceptionHandler;
import kr.chapchap.place.exception.PlaceErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        CorsConfig.class,
        WebMvcConfig.class,
        GlobalExceptionHandler.class
})
@WebMvcTest(ConsumptionCreateController.class)
class ConsumptionCreateApiTest {

    private static final Long USER_ID = 1L;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private ConsumptionCreateService consumptionCreateService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ConsumptionCreateApiTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void 소비_기록을_등록하면_생성된_ID와_획득한_스티커를_반환한다() throws Exception {
        // given
        given(consumptionCreateService.create(any(ConsumptionCreateCommand.class)))
                .willReturn(new ConsumptionCreateInfo(
                        31L,
                        "공통",
                        "눈"
                ));

        // when & then
        mockMvc.perform(post("/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validRequest()))
                        .with(userJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S002"))
                .andExpect(jsonPath("$.data.consumptionId").value(31L))
                .andExpect(jsonPath("$.data.stickerCategory").value("공통"))
                .andExpect(jsonPath("$.data.stickerName").value("눈"));

        ArgumentCaptor<ConsumptionCreateCommand> commandCaptor =
                ArgumentCaptor.forClass(ConsumptionCreateCommand.class);
        then(consumptionCreateService).should().create(commandCaptor.capture());
        ConsumptionCreateCommand command = commandCaptor.getValue();
        assertThat(command.userId()).isEqualTo(USER_ID);
        assertThat(command.receiptImageId()).isEqualTo(15L);
        assertThat(command.place().googlePlaceId()).isEqualTo("ChIJxxxxxxxxxxxxxxxx");
        assertThat(command.place().roadAddress())
                .isEqualTo("서울특별시 강남구 봉은사로 125 1층");
        assertThat(command.place().latitude()).isEqualTo(37.506481);
        assertThat(command.place().longitude()).isEqualTo(127.024551);
        assertThat(command.amount()).isEqualTo(33_000L);
        assertThat(command.category()).isEqualTo("카페");
    }

    @Test
    void 영수증_없이도_소비_기록을_등록할_수_있다() throws Exception {
        // given
        Map<String, Object> request = validRequest();
        request.remove("receiptImageId");
        given(consumptionCreateService.create(any(ConsumptionCreateCommand.class)))
                .willReturn(new ConsumptionCreateInfo(
                        31L,
                        "공통",
                        "눈"
                ));

        // when & then
        mockMvc.perform(post("/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .with(userJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.consumptionId").value(31L))
                .andExpect(jsonPath("$.data.stickerCategory").value("공통"))
                .andExpect(jsonPath("$.data.stickerName").value("눈"));

        ArgumentCaptor<ConsumptionCreateCommand> commandCaptor =
                ArgumentCaptor.forClass(ConsumptionCreateCommand.class);
        then(consumptionCreateService).should().create(commandCaptor.capture());
        assertThat(commandCaptor.getValue().receiptImageId()).isNull();
    }

    @Test
    void 필수_장소_정보와_소비_정보가_유효하지_않으면_등록하지_않는다() throws Exception {
        // given
        Map<String, Object> request = validRequest();
        request.put("receiptImageId", -1);
        request.put("googlePlaceId", " ");
        request.remove("placeName");
        request.remove("roadAddress");
        request.put("latitude", 91);
        request.remove("longitude");
        request.remove("purchaseDate");
        request.remove("purchaseTime");
        request.put("amount", 0);
        request.put("category", " ");

        // when & then
        mockMvc.perform(post("/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .with(userJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.data.receiptImageId").exists())
                .andExpect(jsonPath("$.data.googlePlaceId").exists())
                .andExpect(jsonPath("$.data.placeName").exists())
                .andExpect(jsonPath("$.data.roadAddress").exists())
                .andExpect(jsonPath("$.data.latitude").exists())
                .andExpect(jsonPath("$.data.longitude").exists())
                .andExpect(jsonPath("$.data.purchaseDate").exists())
                .andExpect(jsonPath("$.data.purchaseTime").exists())
                .andExpect(jsonPath("$.data.amount").exists())
                .andExpect(jsonPath("$.data.category").exists());

        then(consumptionCreateService).shouldHaveNoInteractions();
    }

    @Test
    void Access_Token이_없으면_소비_기록을_등록할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(post("/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C004"));

        then(consumptionCreateService).shouldHaveNoInteractions();
    }

    @Test
    void signup_scope로_소비_기록을_등록할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(post("/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validRequest()))
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_signup"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C005"));

        then(consumptionCreateService).shouldHaveNoInteractions();
    }

    @Test
    void 사용자에게_속한_영수증_이미지가_없으면_404를_반환한다() throws Exception {
        // given
        given(consumptionCreateService.create(any(ConsumptionCreateCommand.class)))
                .willThrow(new BusinessException(ConsumptionErrorCode.RECEIPT_IMAGE_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validRequest()))
                        .with(userJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONSUMPTION011"));
    }

    @Test
    void 이미_연결된_영수증_이미지면_409를_반환한다() throws Exception {
        // given
        given(consumptionCreateService.create(any(ConsumptionCreateCommand.class)))
                .willThrow(new BusinessException(
                        ConsumptionErrorCode.RECEIPT_IMAGE_ALREADY_ATTACHED
                ));

        // when & then
        mockMvc.perform(post("/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validRequest()))
                        .with(userJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONSUMPTION012"));
    }

    @Test
    void 도로명주소의_행정동을_찾지_못하면_422를_반환한다() throws Exception {
        // given
        given(consumptionCreateService.create(any(ConsumptionCreateCommand.class)))
                .willThrow(new BusinessException(PlaceErrorCode.ADDRESS_NOT_RESOLVED));

        // when & then
        mockMvc.perform(post("/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validRequest()))
                        .with(userJwt()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PLACE003"));
    }

    private Map<String, Object> validRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("receiptImageId", 15L);
        request.put("googlePlaceId", "ChIJxxxxxxxxxxxxxxxx");
        request.put("placeName", "투썸플레이스 신논현점");
        request.put("roadAddress", "서울특별시 강남구 봉은사로 125 1층");
        request.put("latitude", 37.506481);
        request.put("longitude", 127.024551);
        request.put("purchaseDate", "2026-07-25");
        request.put("purchaseTime", "11:20:00");
        request.put("amount", 33_000L);
        request.put("category", "카페");
        return request;
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
        return jwt()
                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("SCOPE_user"));
    }
}
