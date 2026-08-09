package kr.chapchap.account.api.response;

import jakarta.servlet.http.HttpServletResponse;
import kr.chapchap.account.application.info.AuthenticationInfo;
import kr.chapchap.account.application.info.OAuthClientType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthenticationResponseHandler {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final boolean secureCookie;

    public AuthenticationResponseHandler(
            @Value("${chapchap.auth.refresh-cookie.secure:true}") boolean secureCookie
    ) {
        this.secureCookie = secureCookie;
    }

    public AuthenticationResponse handle(
            AuthenticationInfo info,
            HttpServletResponse response
    ) {
        boolean webClient = info.clientType() == OAuthClientType.WEB;
        if (webClient && info.refreshToken() != null) {
            addRefreshTokenCookie(
                    info.refreshToken(),
                    info.refreshTokenExpiresIn(),
                    response
            );
        }
        boolean appClient = info.clientType() == OAuthClientType.APP;
        return AuthenticationResponse.from(info, appClient);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        addRefreshTokenCookie("", Duration.ZERO, response);
    }

    private void addRefreshTokenCookie(
            String refreshToken,
            Duration maxAge,
            HttpServletResponse response
    ) {
        ResponseCookie cookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
