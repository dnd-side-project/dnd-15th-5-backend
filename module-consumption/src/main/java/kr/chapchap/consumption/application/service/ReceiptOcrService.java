package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.command.ReceiptOcrCommand;
import kr.chapchap.consumption.application.info.ReceiptOcrInfo;
import kr.chapchap.consumption.application.port.ReceiptImageStorage;
import kr.chapchap.consumption.application.port.ReceiptOcrPort;
import kr.chapchap.consumption.domain.entity.ReceiptImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ReceiptOcrService {

    private static final Duration TEMPORARY_IMAGE_TTL = Duration.ofHours(24);

    private final ReceiptImageValidator receiptImageValidator;
    private final ReceiptOcrPort receiptOcrPort;
    private final ReceiptOcrParser receiptOcrParser;
    private final ReceiptImageStorage receiptImageStorage;
    private final ReceiptImageCommandService receiptImageCommandService;
    private final Clock clock;

    public ReceiptOcrInfo recognize(ReceiptOcrCommand command) {
        String contentType = receiptImageValidator.validateAndGetContentType(
                command.receiptImageContent()
        );
        List<String> lines = receiptOcrPort.recognize(
                command.receiptImageContent(),
                contentType
        );
        ReceiptOcrParser.ParsedReceipt parsedReceipt = receiptOcrParser.parse(lines);

        String objectKey = receiptImageStorage.store(
                command.userId(),
                command.receiptImageContent(),
                contentType
        );

        ReceiptImage receiptImage = receiptImageCommandService.saveTemporary(
                command.userId(),
                objectKey,
                contentType,
                command.receiptImageContent().length,
                LocalDateTime.now(clock).plus(TEMPORARY_IMAGE_TTL)
        );

        return new ReceiptOcrInfo(
                receiptImage.getId(),
                parsedReceipt.storeName(),
                parsedReceipt.address(),
                parsedReceipt.purchaseDate(),
                parsedReceipt.purchaseTime(),
                parsedReceipt.amount()
        );
    }
}
