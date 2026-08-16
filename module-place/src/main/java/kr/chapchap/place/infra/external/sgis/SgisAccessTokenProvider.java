package kr.chapchap.place.infra.external.sgis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.exception.CommonErrorCode;
import kr.chapchap.place.infra.config.SgisProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;

@Component
public class SgisAccessTokenProvider {

    private static final Duration EXPIRATION_SAFETY_MARGIN = Duration.ofSeconds(30);
    private static final long EPOCH_MILLISECONDS_THRESHOLD = 100_000_000_000L;

    private final RestClient restClient;
    private final SgisProperties properties;
    private final Clock clock;

    private volatile CachedAccessToken cachedAccessToken;

    public SgisAccessTokenProvider(
            @Qualifier("sgisRestClient") RestClient restClient,
            SgisProperties properties,
            Clock clock
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.clock = clock;
    }

    public String getAccessToken() {
        CachedAccessToken current = cachedAccessToken;
        if (isUsable(current)) {
            return current.value();
        }

        synchronized (this) {
            current = cachedAccessToken;
            if (isUsable(current)) {
                return current.value();
            }
            cachedAccessToken = requestAccessToken();
            return cachedAccessToken.value();
        }
    }

    private boolean isUsable(CachedAccessToken accessToken) {
        return accessToken != null
                && accessToken.expiresAt().isAfter(clock.instant().plus(EXPIRATION_SAFETY_MARGIN));
    }

    private CachedAccessToken requestAccessToken() {
        URI uri = UriComponentsBuilder.fromUri(properties.authenticationUri())
                .queryParam("consumer_key", properties.consumerKey())
                .queryParam("consumer_secret", properties.consumerSecret())
                .build()
                .encode()
                .toUri();

        try {
            AuthenticationResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(AuthenticationResponse.class);
            return extractAccessToken(response);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException | NumberFormatException | DateTimeException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, exception);
        }
    }

    private CachedAccessToken extractAccessToken(AuthenticationResponse response) {
        if (response == null
                || response.errCd() == null
                || response.errCd() != 0
                || response.result() == null
                || !StringUtils.hasText(response.result().accessToken())
                || !StringUtils.hasText(response.result().accessTimeout())) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        long rawTimeout = Long.parseLong(response.result().accessTimeout());
        Instant expiresAt = rawTimeout >= EPOCH_MILLISECONDS_THRESHOLD
                ? Instant.ofEpochMilli(rawTimeout)
                : Instant.ofEpochSecond(rawTimeout);
        if (!expiresAt.isAfter(clock.instant())) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
        return new CachedAccessToken(response.result().accessToken(), expiresAt);
    }

    private record CachedAccessToken(String value, Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthenticationResponse(
            AuthenticationResult result,
            Integer errCd
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthenticationResult(
            String accessToken,
            @JsonProperty("accessTimeout") String accessTimeout
    ) {
    }
}
