package kr.chapchap.consumption.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.chapchap.consumption.application.command.ReceiptOcrCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@Schema(description = "영수증 OCR 요청")
public record ReceiptOcrRequest(
        @Schema(
                description = "OCR 처리할 영수증 이미지 (JPEG, PNG, 최대 5MB, 최대 4096x4096)",
                type = "string",
                format = "binary"
        )
        @NotNull(message = "영수증 이미지는 필수입니다.")
        MultipartFile receiptImage
) {

    public ReceiptOcrCommand toCommand(Long userId) {
        try {
            return new ReceiptOcrCommand(userId, receiptImage.getBytes());
        } catch (IOException exception) {
            throw new UncheckedIOException("영수증 이미지 파일을 읽을 수 없습니다.", exception);
        }
    }
}
