package kr.chapchap.place.infra.external.sgis;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.info.AdministrativeDongInfo;
import kr.chapchap.place.exception.PlaceErrorCode;
import kr.chapchap.place.infra.config.SgisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SgisGeocodingClientTest {

    private static final URI AUTHENTICATION_URI = URI.create("https://sgis.example.com/authentication.json");
    private static final URI GEOCODING_URI = URI.create("https://sgis.example.com/geocodewgs84.json");
    private static final URI REVERSE_GEOCODING_URI = URI.create("https://sgis.example.com/rgeocodewgs84.json");
    private static final String ROAD_ADDRESS = "서울 강남구 테헤란로 1";
    private static final double LATITUDE = 36.343492;
    private static final double LONGITUDE = 127.392925;

    private MockRestServiceServer server;
    private SgisGeocodingClient geocodingClient;
    private URI expectedGeocodingUri;
    private URI expectedReverseGeocodingUri;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        SgisProperties properties = new SgisProperties(
                AUTHENTICATION_URI,
                GEOCODING_URI,
                REVERSE_GEOCODING_URI,
                "consumer-key",
                "consumer-secret",
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        );
        SgisAccessTokenProvider accessTokenProvider = mock(SgisAccessTokenProvider.class);
        when(accessTokenProvider.getAccessToken()).thenReturn("access-token");
        geocodingClient = new SgisGeocodingClient(
                builder.build(),
                properties,
                accessTokenProvider
        );
        expectedGeocodingUri = UriComponentsBuilder.fromUri(GEOCODING_URI)
                .queryParam("accessToken", "access-token")
                .queryParam("address", ROAD_ADDRESS)
                .queryParam("pagenum", 0)
                .queryParam("resultcount", 1)
                .build()
                .encode()
                .toUri();
        expectedReverseGeocodingUri = UriComponentsBuilder.fromUri(REVERSE_GEOCODING_URI)
                .queryParam("accessToken", "access-token")
                .queryParam("x_coor", LONGITUDE)
                .queryParam("y_coor", LATITUDE)
                .queryParam("addr_type", 20)
                .build()
                .encode()
                .toUri();
    }

    @Test
    void 도로명주소를_WGS84_지오코딩하고_행정동_코드와_이름을_반환한다() {
        // given
        server.expect(requestTo(expectedGeocodingUri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "result": {
                                    "totalcount": "1",
                                    "resultdata": [{
                                      "adm_cd": "11680640",
                                      "adm_nm": "역삼1동",
                                      "X": "127.0365",
                                      "Y": "37.5001"
                                    }]
                                  },
                                  "errCd": 0,
                                  "errMsg": "Success"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        AdministrativeDongInfo result = geocodingClient.findByRoadAddress(ROAD_ADDRESS);

        // then
        assertThat(result.code()).isEqualTo("11680640");
        assertThat(result.name()).isEqualTo("역삼1동");
        server.verify();
    }

    @Test
    void SGIS에_주소_검색_결과가_없으면_주소_변환_예외를_던진다() {
        // given
        server.expect(requestTo(expectedGeocodingUri))
                .andRespond(withSuccess(
                        """
                                {
                                  "errCd": -100,
                                  "errMsg": "검색결과 없음"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> geocodingClient.findByRoadAddress(ROAD_ADDRESS))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                PlaceErrorCode.ADDRESS_NOT_RESOLVED
                        )
                );
        server.verify();
    }

    @Test
    void SGIS가_행정동을_null_문자열로_반환하면_외부_서비스_오류로_변환한다() {
        // given
        server.expect(requestTo(expectedGeocodingUri))
                .andRespond(withSuccess(
                        """
                                {
                                  "result": {
                                    "totalcount": "1",
                                    "resultdata": [{
                                      "adm_cd": "null",
                                      "adm_nm": "null"
                                    }]
                                  },
                                  "errCd": 0,
                                  "errMsg": "Success"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> geocodingClient.findByRoadAddress(ROAD_ADDRESS))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
        server.verify();
    }

    @Test
    void SGIS_HTTP_오류는_원인_예외를_보존한_외부_서비스_오류로_변환한다() {
        // given
        server.expect(requestTo(expectedGeocodingUri))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> geocodingClient.findByRoadAddress(ROAD_ADDRESS))
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
    void WGS84_좌표로_행정동을_조회하고_전체_행정동_코드를_반환한다() {
        // given
        server.expect(requestTo(expectedReverseGeocodingUri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "result": [{
                                    "sido_cd": "25",
                                    "sgg_cd": "030",
                                    "emdong_cd": "570",
                                    "emdong_nm": "탄방동"
                                  }],
                                  "errCd": 0,
                                  "errMsg": "Success"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        AdministrativeDongInfo result = geocodingClient.findByCoordinates(
                LATITUDE,
                LONGITUDE
        );

        // then
        assertThat(result.code()).isEqualTo("25030570");
        assertThat(result.name()).isEqualTo("탄방동");
        server.verify();
    }

    @Test
    void SGIS가_좌표_행정동을_null_문자열로_반환하면_외부_서비스_오류로_변환한다() {
        // given
        server.expect(requestTo(expectedReverseGeocodingUri))
                .andRespond(withSuccess(
                        """
                                {
                                  "result": [{
                                    "sido_cd": "null",
                                    "sgg_cd": "null",
                                    "emdong_cd": "null",
                                    "emdong_nm": "null"
                                  }],
                                  "errCd": 0,
                                  "errMsg": "Success"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> geocodingClient.findByCoordinates(LATITUDE, LONGITUDE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
        server.verify();
    }

    @Test
    void SGIS에_좌표_검색_결과가_없으면_주소_변환_예외를_던진다() {
        // given
        server.expect(requestTo(expectedReverseGeocodingUri))
                .andRespond(withSuccess(
                        """
                                {
                                  "errCd": -100,
                                  "errMsg": "검색결과 없음"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> geocodingClient.findByCoordinates(LATITUDE, LONGITUDE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                PlaceErrorCode.ADDRESS_NOT_RESOLVED
                        )
                );
        server.verify();
    }

    @Test
    void SGIS_좌표_조회_처리_오류는_외부_서비스_오류로_변환한다() {
        // given
        server.expect(requestTo(expectedReverseGeocodingUri))
                .andRespond(withSuccess(
                        """
                                {
                                  "errCd": -1,
                                  "errMsg": "서버에서 처리 중 에러가 발생하였습니다."
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> geocodingClient.findByCoordinates(LATITUDE, LONGITUDE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
        server.verify();
    }
}
