package kr.chapchap.account.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.account.application.info.AuthenticationInfo;

@Schema(description = "로그인, 회원가입 완료 또는 토큰 재발급 결과")
public record AuthenticationResponse(
        @Schema(description = "필수 약관 동의가 필요한지 여부", example = "false")
        boolean requiresTermsAgreement,

        @Schema(
                description = "약관 동의가 필요할 때만 발급되는 15분 유효 Signup Token",
                nullable = true
        )
        String signupToken,

        @Schema(
                description = "가입 완료 사용자에게 발급되는 30분 유효 Access Token",
                nullable = true
        )
        String accessToken,

        @Schema(
                description = "APP 응답에만 포함되는 14일 유효 Refresh Token. WEB은 HttpOnly 쿠키로 전달됩니다.",
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
