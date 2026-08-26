package kr.chapchap.place.infra.external.google;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.info.GooglePlaceTextSearchInfo;
import kr.chapchap.place.exception.PlaceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GooglePlaceTextSearchClientTest {

    private static final String BASE_URI = "https://places.googleapis.com/v1";
    private static final String API_KEY = "google-api-key";
    private static final String FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.photos"
    );

    private MockRestServiceServer server;
    private GooglePlaceTextSearchRateLimiter rateLimiter;
    private GooglePlaceTextSearchClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URI)
                .defaultHeader("X-Goog-Api-Key", API_KEY);
        server = MockRestServiceServer.bindTo(builder).build();
        rateLimiter = mock(GooglePlaceTextSearchRateLimiter.class);
        client = new GooglePlaceTextSearchClient(builder.build(), rateLimiter);
    }

    @Test
    void Text_Search를_호출할_때_첫_장소와_첫_사진을_매핑한다() {
        // given
        server.expect(requestTo(BASE_URI + "/places:searchText"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", API_KEY))
                .andExpect(header("X-Goog-FieldMask", FIELD_MASK))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        """
                                {
                                  "textQuery": "투썸플레이스 신논현점 서울특별시 강남구 봉은사로 125",
                                  "pageSize": 1,
                                  "languageCode": "ko"
                                }
                                """
                ))
                .andRespond(withSuccess(
                        """
                                {
                                  "places": [
                                    {
                                      "id": "ChIJ123",
                                      "displayName": {"text": "투썸플레이스 신논현점"},
                                      "formattedAddress": "서울특별시 강남구 봉은사로 125 1층",
                                      "location": {
                                        "latitude": 37.5065,
                                        "longitude": 127.0241
                                      },
                                      "photos": [
                                        {"name": "places/ChIJ123/photos/primary"},
                                        {"name": "places/ChIJ123/photos/secondary"}
                                      ]
                                    },
                                    {
                                      "id": "ChIJ456",
                                      "displayName": {"text": "두 번째 장소"},
                                      "formattedAddress": "서울특별시 강남구 테헤란로 1",
                                      "location": {
                                        "latitude": 37.5,
                                        "longitude": 127.0
                                      }
                                    }
                                  ]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        Optional<GooglePlaceTextSearchInfo> result = client.searchFirst(
                "투썸플레이스 신논현점 서울특별시 강남구 봉은사로 125"
        );

        // then
        assertThat(result).contains(new GooglePlaceTextSearchInfo(
                "ChIJ123",
                "투썸플레이스 신논현점",
                "서울특별시 강남구 봉은사로 125 1층",
                37.5065,
                127.0241,
                "places/ChIJ123/photos/primary"
        ));
        verify(rateLimiter).acquirePermit();
        server.verify();
    }

    @Test
    void Text_Search_결과가_없으면_빈_값을_반환한다() {
        // given
        server.expect(requestTo(BASE_URI + "/places:searchText"))
                .andRespond(withSuccess("{\"places\": []}", MediaType.APPLICATION_JSON));

        // when
        Optional<GooglePlaceTextSearchInfo> result = client.searchFirst("없는 장소");

        // then
        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void Text_Search_장소에_사진이_없으면_photoName_없이_반환한다() {
        // given
        server.expect(requestTo(BASE_URI + "/places:searchText"))
                .andRespond(withSuccess(
                        """
                                {
                                  "places": [{
                                    "id": "ChIJ123",
                                    "displayName": {"text": "장소명"},
                                    "formattedAddress": "서울특별시 강남구 봉은사로 125",
                                    "location": {"latitude": 37.5, "longitude": 127.0}
                                  }]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        GooglePlaceTextSearchInfo result = client.searchFirst("장소명").orElseThrow();

        // then
        assertThat(result.photoName()).isNull();
        server.verify();
    }

    @Test
    void Text_Search_첫_장소의_필수값이_누락되면_외부_서비스_예외를_던진다() {
        // given
        server.expect(requestTo(BASE_URI + "/places:searchText"))
                .andRespond(withSuccess(
                        """
                                {
                                  "places": [{
                                    "id": "ChIJ123",
                                    "displayName": {"text": "장소명"},
                                    "formattedAddress": "서울특별시 강남구 봉은사로 125"
                                  }]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> client.searchFirst("장소명"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
        server.verify();
    }

    @Test
    void Text_Search_HTTP_오류가_발생하면_원인을_보존한_외부_서비스_예외로_변환한다() {
        // given
        server.expect(requestTo(BASE_URI + "/places:searchText"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // when & then
        assertThatThrownBy(() -> client.searchFirst("장소명"))
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
    void Text_Search_월간_한도를_초과하면_Google_API를_호출하지_않는다() {
        // given
        willThrow(new BusinessException(PlaceErrorCode.TEXT_SEARCH_REQUEST_LIMIT_EXCEEDED))
                .given(rateLimiter).acquirePermit();

        // when & then
        assertThatThrownBy(() -> client.searchFirst("장소명"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                PlaceErrorCode.TEXT_SEARCH_REQUEST_LIMIT_EXCEEDED
                        )
                );
        server.verify();
    }

    @Test
    void Text_Search_검색어가_비어_있으면_제한기와_Google_API를_호출하지_않는다() {
        // when
        Optional<GooglePlaceTextSearchInfo> result = client.searchFirst(" ");

        // then
        assertThat(result).isEmpty();
        verify(rateLimiter, never()).acquirePermit();
        server.verify();
    }
}
