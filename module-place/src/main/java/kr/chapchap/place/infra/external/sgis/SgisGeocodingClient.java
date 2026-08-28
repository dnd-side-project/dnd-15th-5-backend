package kr.chapchap.place.infra.external.sgis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.application.info.AdministrativeDongInfo;
import kr.chapchap.place.application.port.AdministrativeDongLookupPort;
import kr.chapchap.place.exception.PlaceErrorCode;
import kr.chapchap.place.infra.config.SgisProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
public class SgisGeocodingClient implements AdministrativeDongLookupPort {

    private static final int SUCCESS_CODE = 0;
    private static final int NO_RESULT_CODE = -100;
    private static final int ADMINISTRATIVE_DONG_ADDRESS_TYPE = 20;

    private final RestClient restClient;
    private final SgisProperties properties;
    private final SgisAccessTokenProvider accessTokenProvider;

    public SgisGeocodingClient(
            @Qualifier("sgisRestClient") RestClient restClient,
            SgisProperties properties,
            SgisAccessTokenProvider accessTokenProvider
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public AdministrativeDongInfo findByRoadAddress(String roadAddress) {
        if (!StringUtils.hasText(roadAddress)) {
            throw new IllegalArgumentException("도로명주소는 비어 있을 수 없습니다.");
        }

        URI uri = UriComponentsBuilder.fromUri(properties.geocodingUri())
                .queryParam("accessToken", accessTokenProvider.getAccessToken())
                .queryParam("address", roadAddress.trim())
                .queryParam("pagenum", 0)
                .queryParam("resultcount", 1)
                .build()
                .encode()
                .toUri();

        try {
            GeocodingResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(GeocodingResponse.class);
            return extractAdministrativeDong(response);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    @Override
    public AdministrativeDongInfo findByCoordinates(double latitude, double longitude) {
        URI uri = UriComponentsBuilder.fromUri(properties.reverseGeocodingUri())
                .queryParam("accessToken", accessTokenProvider.getAccessToken())
                .queryParam("x_coor", longitude)
                .queryParam("y_coor", latitude)
                .queryParam("addr_type", ADMINISTRATIVE_DONG_ADDRESS_TYPE)
                .build()
                .encode()
                .toUri();

        try {
            ReverseGeocodingResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(ReverseGeocodingResponse.class);
            if (response == null || response.errCd() == null) {
                throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            }
            if (response.errCd() == NO_RESULT_CODE) {
                throw new BusinessException(PlaceErrorCode.ADDRESS_NOT_RESOLVED);
            }
            if (response.errCd() != SUCCESS_CODE || response.result() == null) {
                throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            }
            if (response.result().isEmpty()) {
                throw new BusinessException(PlaceErrorCode.ADDRESS_NOT_RESOLVED);
            }

            ReverseGeocodingResultData first = response.result().getFirst();
            if (first == null
                    || !StringUtils.hasText(first.sidoCode())
                    || !StringUtils.hasText(first.sigunguCode())
                    || !StringUtils.hasText(first.administrativeDongCode())
                    || !StringUtils.hasText(first.administrativeDongName())
                    || "null".equalsIgnoreCase(first.sidoCode().trim())
                    || "null".equalsIgnoreCase(first.sigunguCode().trim())
                    || "null".equalsIgnoreCase(first.administrativeDongCode().trim())
                    || "null".equalsIgnoreCase(first.administrativeDongName().trim())) {
                throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            }
            return new AdministrativeDongInfo(
                    first.sidoCode().trim()
                            + first.sigunguCode().trim()
                            + first.administrativeDongCode().trim(),
                    first.administrativeDongName().trim()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private AdministrativeDongInfo extractAdministrativeDong(GeocodingResponse response) {
        if (response == null || response.errCd() == null) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
        if (response.errCd() == NO_RESULT_CODE) {
            throw new BusinessException(PlaceErrorCode.ADDRESS_NOT_RESOLVED);
        }
        if (response.errCd() != SUCCESS_CODE || response.result() == null) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        List<GeocodingResultData> resultData = response.result().resultData();
        if (resultData == null || resultData.isEmpty()) {
            throw new BusinessException(PlaceErrorCode.ADDRESS_NOT_RESOLVED);
        }

        GeocodingResultData first = resultData.getFirst();
        if (first == null
                || !StringUtils.hasText(first.administrativeDongCode())
                || !StringUtils.hasText(first.administrativeDongName())
                || "null".equalsIgnoreCase(first.administrativeDongCode().trim())
                || "null".equalsIgnoreCase(first.administrativeDongName().trim())) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
        return new AdministrativeDongInfo(
                first.administrativeDongCode().trim(),
                first.administrativeDongName().trim()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeocodingResponse(
            GeocodingResult result,
            Integer errCd
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeocodingResult(
            @JsonProperty("resultdata") List<GeocodingResultData> resultData
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeocodingResultData(
            @JsonProperty("adm_cd") String administrativeDongCode,
            @JsonProperty("adm_nm") String administrativeDongName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReverseGeocodingResponse(
            List<ReverseGeocodingResultData> result,
            Integer errCd
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReverseGeocodingResultData(
            @JsonProperty("sido_cd") String sidoCode,
            @JsonProperty("sgg_cd") String sigunguCode,
            @JsonProperty("emdong_cd") String administrativeDongCode,
            @JsonProperty("emdong_nm") String administrativeDongName
    ) {
    }
}
