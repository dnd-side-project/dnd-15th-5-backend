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
    private static final String ROAD_ADDRESS = "서울 강남구 테헤란로 1";

    private MockRestServiceServer server;
    private SgisGeocodingClient geocodingClient;
    private URI expectedGeocodingUri;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        SgisProperties properties = new SgisProperties(
                AUTHENTICATION_URI,
                GEOCODING_URI,
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
}
