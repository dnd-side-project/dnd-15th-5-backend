package kr.chapchap.account.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.account.application.info.AuthenticationInfo;

@Schema(description = """
        인증 처리 결과입니다.

        - 약관 동의 필요: requiresTermsAgreement=true이며 signupToken을 발급합니다.
        - WEB 인증 완료: accessToken을 발급하고 Refresh Token은 HttpOnly 쿠키로 전달합니다.
        - APP 인증 완료: accessToken과 refreshToken을 발급합니다.
        """)
public record AuthenticationResponse(
        @Schema(description = "필수 약관 동의 필요 여부", example = "false")
        boolean requiresTermsAgreement,

        @Schema(
                description = "약관 동의가 필요할 때만 발급되는 15분 유효 Signup Token",
                nullable = true
        )
        String signupToken,

        @Schema(
                description = "인증이 완료된 사용자에게 발급되는 30분 유효 Access Token",
                nullable = true
        )
        String accessToken,

        @Schema(
                description = "APP 인증 완료 시에만 응답 본문에 포함되는 14일 유효 Refresh Token",
                nullable = true
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String refreshToken
) {

    static AuthenticationResponse from(
            AuthenticationInfo info,
            boolean includeRefreshToken
    ) {
        return new AuthenticationResponse(
                info.requiresTermsAgreement(),
                info.signupToken(),
                info.accessToken(),
                includeRefreshToken ? info.refreshToken() : null
        );
    }
}
