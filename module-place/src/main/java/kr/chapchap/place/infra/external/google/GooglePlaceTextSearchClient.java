package kr.chapchap.place.infra.external.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.info.GooglePlaceTextSearchInfo;
import kr.chapchap.place.application.port.GooglePlaceTextSearchPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Component
public class GooglePlaceTextSearchClient implements GooglePlaceTextSearchPort {

    private static final String TEXT_SEARCH_FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.photos"
    );
    private static final int PAGE_SIZE = 1;
    private static final String LANGUAGE_CODE = "ko";

    private final RestClient restClient;
    private final GooglePlaceTextSearchRateLimiter rateLimiter;

    public GooglePlaceTextSearchClient(
            @Qualifier("googlePlacesRestClient") RestClient restClient,
            GooglePlaceTextSearchRateLimiter rateLimiter
    ) {
        this.restClient = restClient;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Optional<GooglePlaceTextSearchInfo> searchFirst(String textQuery) {
        if (!StringUtils.hasText(textQuery)) {
            return Optional.empty();
        }

        try {
            rateLimiter.acquirePermit();
            TextSearchResponse response = restClient.post()
                    .uri(builder -> builder.path("/places:searchText").build())
                    .header("X-Goog-FieldMask", TEXT_SEARCH_FIELD_MASK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TextSearchRequest(textQuery.trim(), PAGE_SIZE, LANGUAGE_CODE))
                    .retrieve()
                    .body(TextSearchResponse.class);
            return extractFirstCandidate(response);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private Optional<GooglePlaceTextSearchInfo> extractFirstCandidate(TextSearchResponse response) {
        if (response == null || response.places() == null || response.places().isEmpty()) {
            return Optional.empty();
        }

        PlaceResponse place = response.places().getFirst();
        if (place == null
                || !StringUtils.hasText(place.id())
                || place.displayName() == null
                || !StringUtils.hasText(place.displayName().text())
                || !StringUtils.hasText(place.formattedAddress())
                || place.location() == null
                || place.location().latitude() == null
                || place.location().longitude() == null
                || !Double.isFinite(place.location().latitude())
                || !Double.isFinite(place.location().longitude())
                || place.location().latitude() < -90
                || place.location().latitude() > 90
                || place.location().longitude() < -180
                || place.location().longitude() > 180) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        String photoName = null;
        if (place.photos() != null && !place.photos().isEmpty()) {
            PhotoResponse photo = place.photos().getFirst();
            if (photo != null && StringUtils.hasText(photo.name())) {
                photoName = photo.name().trim();
            }
        }

        return Optional.of(new GooglePlaceTextSearchInfo(
                place.id().trim(),
                place.displayName().text().trim(),
                place.formattedAddress().trim(),
                place.location().latitude(),
                place.location().longitude(),
                photoName
        ));
    }

    private record TextSearchRequest(String textQuery, int pageSize, String languageCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TextSearchResponse(List<PlaceResponse> places) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlaceResponse(
            String id,
            DisplayNameResponse displayName,
            String formattedAddress,
            LocationResponse location,
            List<PhotoResponse> photos
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DisplayNameResponse(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LocationResponse(Double latitude, Double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhotoResponse(String name) {
    }
}
