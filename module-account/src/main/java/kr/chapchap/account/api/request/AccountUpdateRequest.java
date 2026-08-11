package kr.chapchap.account.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.chapchap.account.application.command.AccountUpdateCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@Schema(description = "내 정보 수정 요청")
public record AccountUpdateRequest(
        @Schema(description = "변경할 닉네임", example = "찹찹이", nullable = true)
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
        @Pattern(regexp = ".*\\S.*", message = "닉네임은 비어 있을 수 없습니다.")
        String nickname,

        @Schema(
                description = "변경할 프로필 이미지 (JPEG, PNG, 최대 5MB, 최대 4096x4096)",
                type = "string",
                format = "binary",
                nullable = true
        )
        MultipartFile profileImage,

        @Schema(description = "기존 프로필 이미지 삭제 여부", example = "false")
        Boolean deleteProfileImage
) {

    public AccountUpdateRequest {
        if (nickname != null) {
            nickname = nickname.trim();
        }
    }

    public AccountUpdateCommand toCommand(Long userId) {
        try {
            return new AccountUpdateCommand(
                    userId,
                    nickname,
                    profileImage == null ? null : profileImage.getBytes(),
                    Boolean.TRUE.equals(deleteProfileImage)
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("프로필 이미지 파일을 읽을 수 없습니다.", exception);
        }
    }
}
