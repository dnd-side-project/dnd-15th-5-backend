package kr.chapchap.account.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "소셜 로그인 코드 교환 요청")
public record LoginCodeExchangeRequest(
        @Schema(
                description = "소셜 로그인 콜백 이후 클라이언트 리디렉션 URI로 전달된 일회용 로그인 코드",
                example = "login-code"
        )
        @NotBlank(message = "로그인 코드는 필수입니다.")
        String loginCode,

        @Schema(
                description = "로그인 시작 시 codeChallenge 생성에 사용한 PKCE codeVerifier",
                example = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        )
        @NotBlank(message = "PKCE 코드 검증값은 필수입니다.")
        @Pattern(
                regexp = "[A-Za-z0-9._~-]{43,128}",
                message = "PKCE 코드 검증값 형식이 올바르지 않습니다."
        )
        String codeVerifier
) {
}
