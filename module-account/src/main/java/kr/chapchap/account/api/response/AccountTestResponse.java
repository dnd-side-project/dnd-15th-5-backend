package kr.chapchap.account.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "계정 API 연결 확인 결과")
public record AccountTestResponse(
        @Schema(description = "응답한 도메인 모듈", example = "account")
        String module,
        @Schema(description = "API 연결 상태", example = "UP")
        String status
) {
}
