package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.core.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

@Component
public class ReceiptImageValidator {

    private static final int MAX_RECEIPT_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_RECEIPT_IMAGE_DIMENSION = 4096;
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String PNG_CONTENT_TYPE = "image/png";

    public String validateAndGetContentType(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ConsumptionErrorCode.INVALID_RECEIPT_IMAGE);
        }
        if (content.length > MAX_RECEIPT_IMAGE_SIZE) {
            throw new BusinessException(ConsumptionErrorCode.RECEIPT_IMAGE_SIZE_EXCEEDED);
        }

        try (ImageInputStream inputStream = ImageIO.createImageInputStream(
                new ByteArrayInputStream(content)
        )) {
            if (inputStream == null) {
                throw new BusinessException(ConsumptionErrorCode.INVALID_RECEIPT_IMAGE);
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
            if (!readers.hasNext()) {
                throw new BusinessException(ConsumptionErrorCode.INVALID_RECEIPT_IMAGE);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(inputStream, true, true);
                String contentType = toContentType(reader.getFormatName());
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0
                        || height <= 0
                        || width > MAX_RECEIPT_IMAGE_DIMENSION
                        || height > MAX_RECEIPT_IMAGE_DIMENSION) {
                    throw new BusinessException(
                            ConsumptionErrorCode.RECEIPT_IMAGE_DIMENSION_EXCEEDED
                    );
                }
                reader.read(0);
                return contentType;
            } finally {
                reader.dispose();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(
                    ConsumptionErrorCode.INVALID_RECEIPT_IMAGE,
                    exception
            );
        }
    }

    private String toContentType(String formatName) {
        return switch (formatName.toLowerCase(Locale.ROOT)) {
            case "jpeg", "jpg" -> JPEG_CONTENT_TYPE;
            case "png" -> PNG_CONTENT_TYPE;
            default -> throw new BusinessException(
                    ConsumptionErrorCode.UNSUPPORTED_RECEIPT_IMAGE_FORMAT
            );
        };
    }
}
