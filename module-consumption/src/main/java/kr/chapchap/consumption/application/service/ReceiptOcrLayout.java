package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.ReceiptOcrDocument;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.Point;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.TextField;

import java.util.ArrayList;
import java.util.List;

/**
 * CLOVA가 단어 단위로 내려주는 결과를 파서에서 사용할 행 단위 데이터로 묶는다.
 *
 * 필드는 CLOVA가 준 순서를 그대로 유지하고 lineBreak가 표시된 곳에서 행을 나눈다.
 * 좌표 기준으로 전부 다시 정렬하면 기울어진 영수증이나 표에서 읽는 순서가 바뀔 수 있다.
 * boundingPoly는 라벨 옆의 값이나 다음 줄로 이어진 주소를 찾을 때 사용한다.
 *
 * 좌표가 없는 입력은 기존 순서와 행 번호로 앞뒤 관계를 판단한다.
 */
final class ReceiptOcrLayout {

    private final List<Line> lines;
    private final Bounds documentBounds;

    private ReceiptOcrLayout(List<Line> lines, Bounds documentBounds) {
        this.lines = List.copyOf(lines);
        this.documentBounds = documentBounds;
    }

    static ReceiptOcrLayout from(ReceiptOcrDocument document) {
        if (document == null || document.fields().isEmpty()) {
            return new ReceiptOcrLayout(List.of(), Bounds.empty());
        }

        List<Line> lines = new ArrayList<>();
        List<SourceField> currentFields = new ArrayList<>();
        List<TextField> fields = document.fields();
        // 좌표만으로 재정렬하면 표 형태 영수증의 읽기 순서가 바뀔 수 있다.
        for (int index = 0; index < fields.size(); index++) {
            TextField field = fields.get(index);
            if (field == null) {
                continue;
            }

            String text = normalize(field.text());
            if (!text.isBlank()) {
                currentFields.add(new SourceField(
                        index,
                        text,
                        normalizeConfidence(field.confidence()),
                        field.confidencePresent(),
                        Bounds.from(field.boundingVertices())
                ));
            }
            if (field.lineBreak() && !currentFields.isEmpty()) {
                lines.add(createLine(lines.size(), currentFields));
                currentFields = new ArrayList<>();
            }
        }
        if (!currentFields.isEmpty()) {
            lines.add(createLine(lines.size(), currentFields));
        }

        Bounds documentBounds = lines.stream()
                .map(Line::bounds)
                .reduce(Bounds.empty(), Bounds::union);
        return new ReceiptOcrLayout(lines, documentBounds);
    }

    List<Line> lines() {
        return lines;
    }

    double verticalRatio(Line line) {
        if (line.bounds().isPresent() && documentBounds.isPresent()) {
            double height = documentBounds.height();
            if (height > 0.0) {
                return clamp(
                        (line.bounds().centerY() - documentBounds.top()) / height,
                        0.0,
                        1.0
                );
            }
        }
        if (lines.size() <= 1) {
            return 0.0;
        }
        return (double) line.index() / (lines.size() - 1);
    }

    boolean isSameRow(Line first, Line second) {
        return isSameRow(first.bounds(), second.bounds());
    }

    // 두 영역이 충분히 겹치거나 중심 높이가 가까우면 같은 표 행으로 본다.
    boolean isSameRow(Bounds first, Bounds second) {
        if (!first.isPresent() || !second.isPresent()) {
            return false;
        }

        double minimumHeight = Math.min(first.height(), second.height());
        double overlap = Math.min(first.bottom(), second.bottom())
                - Math.max(first.top(), second.top());
        if (minimumHeight > 0.0 && overlap / minimumHeight >= 0.3) {
            return true;
        }

        double maximumHeight = Math.max(first.height(), second.height());
        return maximumHeight > 0.0
                && Math.abs(first.centerY() - second.centerY()) <= maximumHeight * 0.65;
    }

    // 주소 라벨과 다음 값처럼 세로 간격이 작고 가로 위치가 이어지는 행을 찾는다.
    boolean isImmediatelyBelow(Line upper, Line lower) {
        Bounds upperBounds = upper.bounds();
        Bounds lowerBounds = lower.bounds();
        if (!upperBounds.isPresent() || !lowerBounds.isPresent()) {
            return lower.index() == upper.index() + 1;
        }
        if (lowerBounds.centerY() <= upperBounds.centerY()) {
            return false;
        }

        double referenceHeight = Math.max(upperBounds.height(), lowerBounds.height());
        double verticalGap = lowerBounds.top() - upperBounds.bottom();
        if (verticalGap > referenceHeight * 2.2) {
            return false;
        }

        double horizontalOverlap = Math.min(upperBounds.right(), lowerBounds.right())
                - Math.max(upperBounds.left(), lowerBounds.left());
        if (horizontalOverlap > 0.0) {
            return true;
        }
        return Math.abs(upperBounds.left() - lowerBounds.left()) <= referenceHeight * 8.0;
    }

    // 이전 행이 오른쪽 끝에서 끝나고 다음 행이 왼쪽에서 시작한 경우 실제 줄바꿈으로 본다.
    boolean isLikelyWrappedContinuation(Line previous, Line next) {
        if (!previous.bounds().isPresent()
                || !next.bounds().isPresent()
                || !documentBounds.isPresent()
                || documentBounds.width() <= 0.0
                || next.bounds().centerY() <= previous.bounds().centerY()
                || !isImmediatelyBelow(previous, next)) {
            return false;
        }

        double previousRightRatio = (previous.bounds().right() - documentBounds.left())
                / documentBounds.width();
        double nextLeftRatio = (next.bounds().left() - documentBounds.left())
                / documentBounds.width();
        return previousRightRatio >= 0.75 && nextLeftRatio <= 0.4;
    }

    double horizontalGap(Line left, Line right) {
        if (!left.bounds().isPresent() || !right.bounds().isPresent()) {
            return Double.POSITIVE_INFINITY;
        }
        return right.bounds().left() - left.bounds().right();
    }

    private static Line createLine(int lineIndex, List<SourceField> sourceFields) {
        List<Field> fields = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        Bounds lineBounds = Bounds.empty();
        double confidenceSum = 0.0;
        int confidenceWeight = 0;

        for (SourceField sourceField : sourceFields) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            int startOffset = text.length();
            text.append(sourceField.text());
            int endOffset = text.length();
            fields.add(new Field(
                    sourceField.providerIndex(),
                    sourceField.text(),
                    sourceField.confidence(),
                    sourceField.confidencePresent(),
                    startOffset,
                    endOffset,
                    sourceField.bounds()
            ));
            lineBounds = lineBounds.union(sourceField.bounds());
            int weight = sourceField.text().codePointCount(0, sourceField.text().length());
            if (sourceField.confidencePresent()) {
                confidenceSum += sourceField.confidence() * weight;
                confidenceWeight += weight;
            }
        }

        double confidence = confidenceWeight == 0 ? 0.0 : confidenceSum / confidenceWeight;
        return new Line(
                lineIndex,
                text.toString(),
                fields,
                lineBounds,
                confidence,
                confidenceWeight > 0
        );
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }

    private static double normalizeConfidence(double confidence) {
        if (!Double.isFinite(confidence)) {
            return 0.0;
        }
        return clamp(confidence, 0.0, 1.0);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    record Line(
            int index,
            String text,
            List<Field> fields,
            Bounds bounds,
            double confidence,
            boolean confidencePresent
    ) {

        Line {
            fields = List.copyOf(fields);
        }

        Bounds boundsForRange(int startOffset, int endOffset) {
            return fields.stream()
                    .filter(field -> field.endOffset() > startOffset
                            && field.startOffset() < endOffset)
                    .map(Field::bounds)
                    .reduce(Bounds.empty(), Bounds::union);
        }

        double confidenceForRange(int startOffset, int endOffset) {
            return fields.stream()
                    .filter(field -> field.endOffset() > startOffset
                            && field.startOffset() < endOffset)
                    .filter(Field::confidencePresent)
                    .mapToDouble(Field::confidence)
                    .average()
                    .orElse(confidence);
        }

        boolean hasConfidenceForRange(int startOffset, int endOffset) {
            return fields.stream()
                    .filter(field -> field.endOffset() > startOffset
                            && field.startOffset() < endOffset)
                    .anyMatch(Field::confidencePresent);
        }
    }

    record Field(
            int providerIndex,
            String text,
            double confidence,
            boolean confidencePresent,
            int startOffset,
            int endOffset,
            Bounds bounds
    ) {
    }

    record Bounds(
            double left,
            double top,
            double right,
            double bottom
    ) {

        static Bounds empty() {
            return new Bounds(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }

        static Bounds from(List<Point> vertices) {
            if (vertices == null || vertices.size() < 2) {
                return empty();
            }

            double left = Double.POSITIVE_INFINITY;
            double top = Double.POSITIVE_INFINITY;
            double right = Double.NEGATIVE_INFINITY;
            double bottom = Double.NEGATIVE_INFINITY;
            for (Point vertex : vertices) {
                if (vertex == null
                        || !Double.isFinite(vertex.x())
                        || !Double.isFinite(vertex.y())) {
                    continue;
                }
                left = Math.min(left, vertex.x());
                top = Math.min(top, vertex.y());
                right = Math.max(right, vertex.x());
                bottom = Math.max(bottom, vertex.y());
            }
            if (!Double.isFinite(left)
                    || !Double.isFinite(top)
                    || !Double.isFinite(right)
                    || !Double.isFinite(bottom)
                    || right <= left
                    || bottom <= top) {
                return empty();
            }
            return new Bounds(left, top, right, bottom);
        }

        boolean isPresent() {
            return Double.isFinite(left)
                    && Double.isFinite(top)
                    && Double.isFinite(right)
                    && Double.isFinite(bottom);
        }

        Bounds union(Bounds other) {
            if (other == null || !other.isPresent()) {
                return this;
            }
            if (!isPresent()) {
                return other;
            }
            return new Bounds(
                    Math.min(left, other.left),
                    Math.min(top, other.top),
                    Math.max(right, other.right),
                    Math.max(bottom, other.bottom)
            );
        }

        double width() {
            return isPresent() ? Math.max(0.0, right - left) : 0.0;
        }

        double height() {
            return isPresent() ? Math.max(0.0, bottom - top) : 0.0;
        }

        double centerX() {
            return isPresent() ? (left + right) / 2.0 : Double.NaN;
        }

        double centerY() {
            return isPresent() ? (top + bottom) / 2.0 : Double.NaN;
        }
    }

    private record SourceField(
            int providerIndex,
            String text,
            double confidence,
            boolean confidencePresent,
            Bounds bounds
    ) {
    }
}
