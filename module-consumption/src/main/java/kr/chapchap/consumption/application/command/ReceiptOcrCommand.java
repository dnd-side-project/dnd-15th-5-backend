package kr.chapchap.consumption.application.command;

public record ReceiptOcrCommand(
        Long userId,
        byte[] receiptImageContent
) {
}
