package kr.chapchap.place.infra.external.google;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.info.PlacePhotoInfo.PhotoMetadataInfo;
import kr.chapchap.place.exception.PlaceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GooglePlacePhotoClientTest {

    private static final String BASE_URI = "https://places.googleapis.com/v1";
    private static final String API_KEY = "google-api-key";
    private static final String PHOTO_NAME = "places/ChIJ123/photos/ATKogpe_abc-123";

    private MockRestServiceServer server;
    private GooglePlacePhotoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URI)
                .defaultHeader("X-Goog-Api-Key", API_KEY);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GooglePlacePhotoClient(builder.build());
    }

    @Test
    void Place_Details에서_photos를_조회할_때_첫_사진의_name과_googleMapsUri를_반환한다() {
        // given
        server.expect(requestTo(BASE_URI + "/places/ChIJ123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Goog-Api-Key", API_KEY))
                .andExpect(header("X-Goog-FieldMask", "photos"))
                .andRespond(withSuccess(
                        """
                                {
                                  "photos": [
                                    {
                                      "name": "places/ChIJ123/photos/ATKogpe_abc-123",
                                      "googleMapsUri": "https://maps.google.com/photo"
                                    },
                                    {
                                      "name": "places/ChIJ123/photos/second-photo"
                                    }
                                  ]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        Optional<PhotoMetadataInfo> result = client.findPrimaryPhoto("ChIJ123");

        // then
        assertThat(result).isPresent();
        PhotoMetadataInfo photo = result.orElseThrow();
        assertThat(photo.name()).isEqualTo(PHOTO_NAME);
        assertThat(photo.googleMapsUri()).isEqualTo("https://maps.google.com/photo");
        server.verify();
    }

    @Test
    void Place_Details에서_photos를_조회할_때_첫_사진에_googleMapsUri가_없으면_예외를_던진다() {
        // given
        server.expect(requestTo(BASE_URI + "/places/ChIJ123"))
                .andRespond(withSuccess(
                        """
                                {
                                  "photos": [{"name": "places/ChIJ123/photos/ATKogpe_abc-123"}]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> client.findPrimaryPhoto("ChIJ123"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
        server.verify();
    }

    @Test
    void Place_Details에서_photos를_조회할_때_장소에_사진이_없으면_빈_값을_반환한다() {
        // given
        server.expect(requestTo(BASE_URI + "/places/ChIJ123"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // when
        Optional<PhotoMetadataInfo> result = client.findPrimaryPhoto("ChIJ123");

        // then
        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void Place_Details에서_photos를_조회할_때_HTTP_오류가_발생하면_원인을_보존한_외부_서비스_오류로_변환한다() {
        // given
        server.expect(requestTo(BASE_URI + "/places/ChIJ123"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> client.findPrimaryPhoto("ChIJ123"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                    assertThat(exception.getCause()).isInstanceOf(RestClientResponseException.class);
                });
        server.verify();
    }

    @Test
    void Photo_Media를_조회할_때_HTTPS_photoUri를_반환한다() {
        // given
        server.expect(requestTo(
                        BASE_URI + "/places/ChIJ123/photos/ATKogpe_abc-123/media"
                                + "?maxWidthPx=400&skipHttpRedirect=true"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Goog-Api-Key", API_KEY))
                .andRespond(withSuccess(
                        """
                                {
                                  "name": "places/ChIJ123/photos/ATKogpe_abc-123/media",
                                  "photoUri": "https://lh3.googleusercontent.com/photo"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when
        URI result = client.resolvePhotoUri(PHOTO_NAME, 400);

        // then
        assertThat(result).isEqualTo(URI.create("https://lh3.googleusercontent.com/photo"));
        server.verify();
    }

    @Test
    void Photo_Media를_조회할_때_HTTP_photoUri를_반환하면_외부_서비스_오류가_발생한다() {
        // given
        server.expect(requestTo(
                        BASE_URI + "/places/ChIJ123/photos/ATKogpe_abc-123/media"
                                + "?maxWidthPx=400&skipHttpRedirect=true"
                ))
                .andRespond(withSuccess(
                        """
                                {"photoUri": "http://images.example.com/photo"}
                                """,
                        MediaType.APPLICATION_JSON
                ));

        // when & then
        assertThatThrownBy(() -> client.resolvePhotoUri(PHOTO_NAME, 400))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                        )
                );
        server.verify();
    }

    @Test
    void Photo_Media를_조회할_때_404를_반환하면_사진을_조회할_수_없다() {
        // given
        server.expect(requestTo(
                        BASE_URI + "/places/ChIJ123/photos/ATKogpe_abc-123/media"
                                + "?maxWidthPx=400&skipHttpRedirect=true"
                ))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> client.resolvePhotoUri(PHOTO_NAME, 400))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PlaceErrorCode.PHOTO_NOT_FOUND)
                );
        server.verify();
    }

    @Test
    void Photo_Media를_조회할_때_5xx를_반환하면_원인을_보존한_외부_서비스_오류가_발생한다() {
        // given
        server.expect(requestTo(
                        BASE_URI + "/places/ChIJ123/photos/ATKogpe_abc-123/media"
                                + "?maxWidthPx=400&skipHttpRedirect=true"
                ))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // when & then
        assertThatThrownBy(() -> client.resolvePhotoUri(PHOTO_NAME, 400))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                    assertThat(exception.getCause()).isInstanceOf(RestClientResponseException.class);
                });
        server.verify();
    }
}
