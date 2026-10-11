package kr.chapchap.consumption.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.chapchap.consumption.application.command.ConsumptionImageUploadCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@Schema(description = "소비기록 이미지 업로드 요청")
public record ConsumptionImageUploadRequest(
        @Schema(
                description = "프론트에서 가공한 소비기록 이미지 (JPEG, PNG, 최대 5MB, 최대 4096x4096)",
                type = "string",
                format = "binary"
        )
        @NotNull(message = "소비기록 이미지는 필수입니다.")
        MultipartFile image
) {

    public ConsumptionImageUploadCommand toCommand(Long userId) {
        try {
            return new ConsumptionImageUploadCommand(userId, image.getBytes());
        } catch (IOException exception) {
            throw new UncheckedIOException("소비기록 이미지 파일을 읽을 수 없습니다.", exception);
        }
    }
}
