package kr.chapchap.place.infra.external.sgis;

import kr.chapchap.place.infra.config.SgisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SgisAccessTokenProviderTest {

    private static final URI AUTHENTICATION_URI = URI.create("https://sgis.example.com/authentication.json");
    private static final URI GEOCODING_URI = URI.create("https://sgis.example.com/geocodewgs84.json");
    private static final URI REVERSE_GEOCODING_URI = URI.create("https://sgis.example.com/rgeocodewgs84.json");
    private static final Instant NOW = Instant.parse("2026-08-16T00:00:00Z");

    private MockRestServiceServer server;
    private MutableClock clock;
    private SgisAccessTokenProvider tokenProvider;
    private URI expectedAuthenticationUri;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        clock = new MutableClock(NOW, ZoneOffset.UTC);
        SgisProperties properties = new SgisProperties(
                AUTHENTICATION_URI,
                GEOCODING_URI,
                REVERSE_GEOCODING_URI,
                "consumer-key",
                "consumer-secret",
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        );
        tokenProvider = new SgisAccessTokenProvider(builder.build(), properties, clock);
        expectedAuthenticationUri = UriComponentsBuilder.fromUri(AUTHENTICATION_URI)
                .queryParam("consumer_key", "consumer-key")
                .queryParam("consumer_secret", "consumer-secret")
                .build()
                .encode()
                .toUri();
    }

    @Test
    void 밀리초_epoch인_accessTimeout을_해석하고_만료_전까지_토큰을_캐시한다() {
        // given
        expectToken("first-token", NOW.plusSeconds(60).toEpochMilli());
        expectToken("second-token", NOW.plusSeconds(3600).toEpochMilli());

        // when
        String first = tokenProvider.getAccessToken();
        String cached = tokenProvider.getAccessToken();
        clock.advance(Duration.ofSeconds(31));
        String refreshed = tokenProvider.getAccessToken();

        // then
        assertThat(first).isEqualTo("first-token");
        assertThat(cached).isEqualTo("first-token");
        assertThat(refreshed).isEqualTo("second-token");
        server.verify();
    }

    @Test
    void 초_epoch인_accessTimeout도_해석해_토큰을_갱신한다() {
        // given
        expectToken("first-token", NOW.plusSeconds(60).getEpochSecond());
        expectToken("second-token", NOW.plusSeconds(3600).getEpochSecond());

        // when
        String first = tokenProvider.getAccessToken();
        clock.advance(Duration.ofSeconds(31));
        String refreshed = tokenProvider.getAccessToken();

        // then
        assertThat(first).isEqualTo("first-token");
        assertThat(refreshed).isEqualTo("second-token");
        server.verify();
    }

    private void expectToken(String accessToken, long accessTimeout) {
        server.expect(requestTo(expectedAuthenticationUri))
                .andRespond(withSuccess(
                        """
                                {
                                  "result": {
                                    "accessToken": "%s",
                                    "accessTimeout": "%d"
                                  },
                                  "errCd": 0
                                }
                                """.formatted(accessToken, accessTimeout),
                        MediaType.APPLICATION_JSON
                ));
    }

    private static final class MutableClock extends Clock {

        private Instant currentInstant;
        private final ZoneId zone;

        private MutableClock(Instant currentInstant, ZoneId zone) {
            this.currentInstant = currentInstant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        private void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }
    }
}
