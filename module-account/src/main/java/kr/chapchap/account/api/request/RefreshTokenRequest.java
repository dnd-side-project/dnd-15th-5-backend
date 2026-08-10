package kr.chapchap.account.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "APP Refresh Token 요청")
public record RefreshTokenRequest(
        @Schema(description = "APP 로그인 또는 이전 재발급에서 받은 Refresh Token")
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
