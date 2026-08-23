package kr.chapchap.account.infra.config;

import kr.chapchap.account.application.info.OAuthClientType;
import kr.chapchap.account.application.port.OAuthClientRedirectPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RequiredArgsConstructor
@Component
public class OAuthClientRedirectResolver implements OAuthClientRedirectPort {

    private final OAuthClientRedirectProperties properties;

    @Override
    public URI createLoginRedirect(OAuthClientType clientType, String loginCode) {
        return UriComponentsBuilder.fromUri(resolveRedirectUri(clientType))
                .queryParam("loginCode", loginCode)
                .build()
                .encode()
                .toUri();
    }

    @Override
    public URI createWithdrawalRedirect(OAuthClientType clientType) {
        return UriComponentsBuilder.fromUri(resolveRedirectUri(clientType))
                .queryParam("withdrawal", "success")
                .build()
                .encode()
                .toUri();
    }

    @Override
    public URI createErrorRedirect(OAuthClientType clientType, String errorCode) {
        return UriComponentsBuilder.fromUri(resolveRedirectUri(clientType))
                .queryParam("error", errorCode)
                .build()
                .encode()
                .toUri();
    }

    private URI resolveRedirectUri(OAuthClientType clientType) {
        return switch (clientType) {
            case WEB -> properties.webRedirectUri();
            case WEB_LOCAL -> properties.localWebRedirectUri();
            case APP -> properties.appRedirectUri();
        };
    }
}
