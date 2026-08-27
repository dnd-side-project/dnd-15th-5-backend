package kr.chapchap.consumption.infra.external.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.Point;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.TextField;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.consumption.infra.config.ClovaOcrProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClovaOcrClientTest {

    private static final URI INVOKE_URL = URI.create("https://ocr.example.com/custom/general");
    private static final String SECRET_KEY = "clova-secret-key";
    private static final String API_GATEWAY_KEY = "api-gateway-key";
    private static final byte[] IMAGE_CONTENT = new byte[]{1, 2, 3};
    private static final Instant NOW = Instant.parse("2026-08-16T00:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer server;
    private ClovaOcrRateLimiter rateLimiter;
    private ClovaOcrClient clovaOcrClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        rateLimiter = mock(ClovaOcrRateLimiter.class);
        clovaOcrClient = new ClovaOcrClient(
                builder.build(),
                new ClovaOcrProperties(
                        INVOKE_URL,
                        SECRET_KEY,
                        API_GATEWAY_KEY,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(15),
                        Duration.ofMillis(1100),
                        Duration.ofSeconds(5)
                ),
                rateLimiter,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void CLOVA_일반_OCR을_호출하고_텍스트와_좌표와_신뢰도를_반환한다() {
        // given
        server.expect(requestTo(INVOKE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-OCR-SECRET", SECRET_KEY))
                .andExpect(header("x-ncp-apigw-api-key", API_GATEWAY_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    JsonNode root = objectMapper.readTree(body);
                    assertThat(root.path("version").asText()).isEqualTo("V2");
                    assertThat(root.path("lang").asText()).isEqualTo("ko");
                    assertThat(root.path("timestamp").asLong()).isEqualTo(NOW.toEpochMilli());
                    assertThat(UUID.fromString(root.path("requestId").asText())).isNotNull();
                    assertThat(root.path("images").size()).isEqualTo(1);
                    assertThat(root.path("images").get(0).path("format").asText())
                            .isEqualTo("png");
                    assertThat(root.path("images").get(0).path("name").asText())
                            .isEqualTo("receipt");
                    assertThat(root.path("images").get(0).path("data").asText())
                            .isEqualTo(Base64.getEncoder().encodeToString(IMAGE_CONTENT));
                })
                .andRespond(withSuccess(
                        """
                                {
                                  "images": [{
                                    "inferResult": "SUCCESS",
                                    "fields": [
                                      {
                                        "inferText": "투썸플레이스",
                                        "inferConfidence": 0.9981,
                                        "lineBreak": false,
                                        "boundingPoly": {
                                          "vertices": [
                                            {"x": 10.5, "y": 20.25},
                                            {"x": 110.5, "y": 20.25},
                                            {"x": 110.5, "y": 40.25},
                                            {"x": 10.5, "y": 40.25}
                                          ]
                                        }
                                      },
                                      {
                                        "inferText": "신논현점",
                                        "inferConfidence": 0.9972,
                                        "lineBreak": true,
                                        "boundingPoly": {
                                          "vertices": [
                                            {"x": 120.0, "y": 20.25},
                                            {"x": 190.0, "y": 20.25},
                                            {"x": 190.0, "y": 40.25},
                                            {"x": 120.0, "y": 40.25}
                                          ]
                                        }
                                      },
                                      {"inferText": "결제금액", "inferConfidence": 0.9963, "lineBreak": false},
                                      {"inferText": "33,000원", "inferConfidence": 0.9994, "lineBreak": true}
                                    ]
                                  }]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        ReceiptOcrDocument result = clovaOcrClient.recognize(IMAGE_CONTENT, "image/png");

        // then
        assertThat(result.lines()).containsExactly(
                "투썸플레이스 신논현점",
                "결제금액 33,000원"
        );
        assertThat(result.fields().getFirst()).isEqualTo(new TextField(
                "투썸플레이스",
                0.9981,
                false,
                List.of(
                        new Point(10.5, 20.25),
                        new Point(110.5, 20.25),
                        new Point(110.5, 40.25),
                        new Point(10.5, 40.25)
                )
        ));
        assertThat(result.fields()).extracting(TextField::confidence)
                .containsExactly(0.9981, 0.9972, 0.9963, 0.9994);
        assertThat(result.fields()).extracting(TextField::lineBreak)
                .containsExactly(false, true, false, true);
        then(rateLimiter).should().awaitPermit();
        server.verify();
    }

    @Test
    void 호출_대기_한도를_초과하면_CLOVA를_호출하지_않는다() {
        // given
        willThrow(new BusinessException(
                ConsumptionErrorCode.RECEIPT_OCR_REQUEST_LIMIT_EXCEEDED
        )).given(rateLimiter).awaitPermit();

        // when & then
        assertThatThrownBy(() -> clovaOcrClient.recognize(IMAGE_CONTENT, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ConsumptionErrorCode.RECEIPT_OCR_REQUEST_LIMIT_EXCEEDED
                        )
                );
        server.verify();
    }

    @Test
    void 인식_성공이지만_텍스트가_없으면_빈_목록을_반환한다() {
        // given
        server.expect(requestTo(INVOKE_URL))
                .andRespond(withSuccess(
                        """
                                {"images": [{"inferResult": "SUCCESS", "fields": []}]}
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        ReceiptOcrDocument result = clovaOcrClient.recognize(IMAGE_CONTENT, "image/jpeg");

        // then
        assertThat(result.fields()).isEmpty();
        assertThat(result.lines()).isEmpty();
        server.verify();
    }

    @Test
    void 좌표와_신뢰도가_누락되면_누락_상태를_보존한다() {
        // given
        server.expect(requestTo(INVOKE_URL))
                .andRespond(withSuccess(
                        """
                                {
                                  "images": [{
                                    "inferResult": "SUCCESS",
                                    "fields": [{
                                      "inferText": "합계",
                                      "lineBreak": true
                                    }]
                                  }]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        ReceiptOcrDocument result = clovaOcrClient.recognize(
                IMAGE_CONTENT,
                "image/jpeg"
        );

        // then
        assertThat(result.fields()).containsExactly(
                new TextField("합계", 0.0, true, List.of(), false)
        );
        assertThat(result.lines()).containsExactly("합계");
        server.verify();
    }

    @Test
    void 일부_꼭짓점만_유효한_polygon은_좌표_없음으로_처리한다() {
        // given
        server.expect(requestTo(INVOKE_URL))
                .andRespond(withSuccess(
                        """
                                {
                                  "images": [{
                                    "inferResult": "SUCCESS",
                                    "fields": [{
                                      "inferText": "합계",
                                      "inferConfidence": 0.99,
                                      "lineBreak": true,
                                      "boundingPoly": {
                                        "vertices": [
                                          {"x": 10.0, "y": 20.0},
                                          {"x": 110.0, "y": 20.0},
                                          {"x": null, "y": 40.0},
                                          {"x": 10.0, "y": 40.0}
                                        ]
                                      }
                                    }]
                                  }]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        ReceiptOcrDocument result = clovaOcrClient.recognize(
                IMAGE_CONTENT,
                "image/jpeg"
        );

        // then
        assertThat(result.fields().getFirst().boundingVertices()).isEmpty();
        server.verify();
    }

    @Test
    void CLOVA가_인식하지_못하면_영수증_인식_오류로_변환한다() {
        // given
        server.expect(requestTo(INVOKE_URL))
                .andRespond(withSuccess(
                        """
                                {"images": [{"inferResult": "FAILURE", "fields": []}]}
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> clovaOcrClient.recognize(IMAGE_CONTENT, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ConsumptionErrorCode.RECEIPT_OCR_RECOGNITION_FAILED
                        )
                );
        server.verify();
    }

    @Test
    void CLOVA가_처리_오류를_반환하면_외부_서비스_오류로_변환한다() {
        // given
        server.expect(requestTo(INVOKE_URL))
                .andRespond(withSuccess(
                        """
                                {"images": [{"inferResult": "ERROR", "fields": []}]}
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> clovaOcrClient.recognize(IMAGE_CONTENT, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
        server.verify();
    }

    @Test
    void CLOVA가_이미지_인식_결과를_누락하면_외부_서비스_오류로_변환한다() {
        // given
        server.expect(requestTo(INVOKE_URL))
                .andRespond(withSuccess(
                        """
                                {"images": [null]}
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> clovaOcrClient.recognize(IMAGE_CONTENT, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
        server.verify();
    }

    @Test
    void CLOVA_HTTP_오류는_원인_예외를_보존한_외부_서비스_오류로_변환한다() {
        // given
        server.expect(requestTo(INVOKE_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> clovaOcrClient.recognize(IMAGE_CONTENT, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(
                            CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                    );
                    assertThat(exception.getCause()).isInstanceOf(
                            RestClientResponseException.class
                    );
                });
        server.verify();
    }

    @Test
    void API_Gateway의_요청_제한은_외부_서비스_오류로_변환한다() {
        // given
        server.expect(requestTo(INVOKE_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        // when & then
        assertThatThrownBy(() -> clovaOcrClient.recognize(IMAGE_CONTENT, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
        server.verify();
    }
}
