package kr.chapchap.config;

import kr.chapchap.core.test.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ActiveProfiles({"test", "monitoring"})
@AutoConfigureObservability
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0"
)
class MonitoringIntegrationTest {

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 내부_포트에서_인증_없이_JVM과_HTTP_지표를_수집한다() {
        // given
        ResponseEntity<String> apiResponse = restTemplate.getForEntity(
                "http://localhost:" + serverPort + "/v1/accounts/me",
                String.class
        );
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // when
        ResponseEntity<String> metrics = restTemplate.getForEntity(
                "http://localhost:" + managementPort + "/actuator/prometheus",
                String.class
        );

        // then
        assertThat(managementPort).isNotEqualTo(serverPort);
        assertThat(metrics.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(metrics.getBody()).contains(
                "jvm_memory_used_bytes",
                "jvm_gc_",
                "http_server_requests_seconds_count",
                "http_server_requests_seconds_bucket",
                "status=\"401\"",
                "application=\"chapchap-app-server\"",
                "environment=\"prod\""
        );
    }

    @Test
    void 공개_API_포트는_인증된_사용자에게도_지표를_노출하지_않는다() {
        // given
        HttpEntity<Void> request = authenticatedRequest();

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + serverPort + "/actuator/prometheus",
                HttpMethod.GET,
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void 내부_포트에서도_환경변수_엔드포인트를_노출하지_않는다() {
        // given
        HttpEntity<Void> request = authenticatedRequest();

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + managementPort + "/actuator/env",
                HttpMethod.GET,
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void 컨테이너_헬스체크는_내부_포트에서_응답한다() {
        // given
        String url = "http://localhost:" + managementPort + "/actuator/health";

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    private HttpEntity<Void> authenticatedRequest() {
        String token = "monitoring-test-user";
        given(jwtDecoder.decode(token)).willReturn(Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("1")
                .claim("scope", "user")
                .build());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
