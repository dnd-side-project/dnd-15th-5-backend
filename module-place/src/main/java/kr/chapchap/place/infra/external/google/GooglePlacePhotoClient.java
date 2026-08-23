package kr.chapchap.place.infra.external.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.info.PlacePhotoInfo.PhotoMetadataInfo;
import kr.chapchap.place.application.port.PlacePhotoPort;
import kr.chapchap.place.exception.PlaceErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
public class GooglePlacePhotoClient implements PlacePhotoPort {

    private static final String PHOTO_FIELD_MASK = "photos";
    private static final int MAX_PHOTO_WIDTH_PX = 4800;

    private final RestClient restClient;
    private final GooglePlacePhotoRateLimiter rateLimiter;

    public GooglePlacePhotoClient(
            @Qualifier("googlePlacesRestClient") RestClient restClient,
            GooglePlacePhotoRateLimiter rateLimiter
    ) {
        this.restClient = restClient;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Optional<PhotoMetadataInfo> findPrimaryPhoto(String googlePlaceId) {
        if (!StringUtils.hasText(googlePlaceId)) {
            return Optional.empty();
        }

        try {
            PlaceDetailsResponse response = restClient.get()
                    .uri(builder -> builder
                            .pathSegment("places", googlePlaceId.trim())
                            .build())
                    .header("X-Goog-FieldMask", PHOTO_FIELD_MASK)
                    .retrieve()
                    .body(PlaceDetailsResponse.class);
            return extractPrimaryPhoto(response);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    @Override
    public URI resolvePhotoUri(String photoName, int maxWidthPx) {
        try {
            String[] photoNameSegments = requirePhotoNameSegments(photoName);
            if (maxWidthPx < 1 || maxWidthPx > MAX_PHOTO_WIDTH_PX) {
                throw new IllegalArgumentException("Google Place 사진 너비가 유효하지 않습니다.");
            }

            rateLimiter.acquirePermit();
            PhotoMediaResponse response = restClient.get()
                    .uri(builder -> builder
                            .pathSegment(photoNameSegments)
                            .pathSegment("media")
                            .queryParam("maxWidthPx", maxWidthPx)
                            .queryParam("skipHttpRedirect", true)
                            .build())
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 404,
                            (request, responseError) -> {
                                throw new BusinessException(PlaceErrorCode.PHOTO_NOT_FOUND);
                            }
                    )
                    .body(PhotoMediaResponse.class);
            return requireHttpsUri(response != null ? response.photoUri() : null);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private Optional<PhotoMetadataInfo> extractPrimaryPhoto(PlaceDetailsResponse response) {
        if (response == null || response.photos() == null || response.photos().isEmpty()) {
            return Optional.empty();
        }

        PhotoResponse photo = response.photos().getFirst();
        if (photo == null || !StringUtils.hasText(photo.name())) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        return Optional.of(new PhotoMetadataInfo(
                photo.name().trim(),
                requireHttpsUri(photo.googleMapsUri()).toString()
        ));
    }

    private String[] requirePhotoNameSegments(String photoName) {
        if (!StringUtils.hasText(photoName)) {
            throw new IllegalArgumentException("Google Place 사진 이름이 비어 있습니다.");
        }

        String[] segments = photoName.split("/", -1);
        if (segments.length != 4
                || !"places".equals(segments[0])
                || segments[1].isBlank()
                || !"photos".equals(segments[2])
                || segments[3].isBlank()) {
            throw new IllegalArgumentException("Google Place 사진 이름 형식이 유효하지 않습니다.");
        }
        return segments;
    }

    private URI requireHttpsUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        try {
            URI parsedUri = URI.create(uri);
            if (!parsedUri.isAbsolute()
                    || !"https".equalsIgnoreCase(parsedUri.getScheme())
                    || !StringUtils.hasText(parsedUri.getHost())) {
                throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            }
            return parsedUri;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlaceDetailsResponse(List<PhotoResponse> photos) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhotoResponse(
            String name,
            String googleMapsUri
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhotoMediaResponse(String photoUri) {
    }
}
