package kr.chapchap.account.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.account.application.info.AccountInfo;

@Schema(description = "내 정보 조회 결과")
public record AccountResponse(
        @Schema(description = "사용자 식별자", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "찹찹이")
        String nickname,

        @Schema(description = "프로필 이미지 URL", nullable = true)
        String profileImageUrl
) {

    public static AccountResponse from(AccountInfo info) {
        return new AccountResponse(
                info.userId(),
                info.nickname(),
                info.profileImageUrl()
        );
    }
}
