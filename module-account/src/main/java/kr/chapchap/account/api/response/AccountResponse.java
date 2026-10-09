package kr.chapchap.account.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.chapchap.account.application.info.AccountInfo;

@Schema(description = "내 정보 조회 결과")
public record AccountResponse(
        @Schema(description = "사용자 식별자", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "찹찹이")
        String nickname,

        @Schema(description = "기존 파일 업로드 API의 이미지 URL. 현재 조회에서는 사용하지 않습니다.", nullable = true)
        String profileImageUrl,

        @Schema(description = "현재 기본 프로필 이미지 코드", example = "BLUE")
        String profileImageCode
) {

    public static AccountResponse from(AccountInfo info) {
        return new AccountResponse(
                info.userId(),
                info.nickname(),
                info.profileImageUrl(),
                info.profileImageCode()
        );
    }
}
