package kr.chapchap.consumption.application.info;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record ReceiptOcrDocument(List<TextField> fields) {

    private static final ReceiptOcrDocument EMPTY = new ReceiptOcrDocument(List.of());

    public ReceiptOcrDocument {
        fields = List.copyOf(Objects.requireNonNull(fields));
    }

    public static ReceiptOcrDocument empty() {
        return EMPTY;
    }

    public static ReceiptOcrDocument fromLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return empty();
        }

        List<TextField> fields = lines.stream()
                .filter(line -> line != null && !line.isBlank())
                .map(line -> new TextField(line, 0.0, true, List.of(), false))
                .toList();
        return fields.isEmpty() ? empty() : new ReceiptOcrDocument(fields);
    }

    public List<String> lines() {
        if (fields.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (TextField field : fields) {
            if (field.text() != null && !field.text().isBlank()) {
                if (!currentLine.isEmpty()) {
                    currentLine.append(' ');
                }
                currentLine.append(field.text().trim());
            }
            if (field.lineBreak() && !currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return List.copyOf(lines);
    }

    public record TextField(
            String text,
            double confidence,
            boolean lineBreak,
            List<Point> boundingVertices,
            boolean confidencePresent
    ) {

        public TextField(
                String text,
                double confidence,
                boolean lineBreak,
                List<Point> boundingVertices
        ) {
            this(text, confidence, lineBreak, boundingVertices, true);
        }

        public TextField {
            text = Objects.requireNonNull(text);
            boundingVertices = List.copyOf(Objects.requireNonNull(boundingVertices));
            if (!Double.isFinite(confidence)) {
                confidence = 0.0;
                confidencePresent = false;
            }
        }
    }

    public record Point(double x, double y) {
    }
}
