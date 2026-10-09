package kr.chapchap.account.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "이름과 기본 프로필 이미지 수정 요청")
public record ProfileUpdateRequest(
        @Schema(description = "한글·자모·공백 허용, 앞뒤 공백 제거 후 1~16자", example = "찹찹이ㅋㅋ")
        @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
        @Size(max = 16, message = "닉네임은 16자를 초과할 수 없습니다.")
        @Pattern(regexp = "[가-힣ㄱ-ㅎㅏ-ㅣ ]+", message = "닉네임은 한글, 자음, 모음과 공백만 사용할 수 있습니다.")
        String nickname,

        @Schema(
                description = "선택한 기본 프로필 이미지",
                example = "BLUE",
                allowableValues = {"BLUE", "SKY_BLUE", "PINK", "YELLOW", "TEAL", "RED"}
        )
        @NotBlank(message = "프로필 이미지 코드를 선택해야 합니다.")
        String profileImageCode
) {
    public ProfileUpdateRequest {
        if (nickname != null) {
            nickname = nickname.trim();
        }
    }
}
