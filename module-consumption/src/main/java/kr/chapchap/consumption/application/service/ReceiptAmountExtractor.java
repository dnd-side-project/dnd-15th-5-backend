package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.service.ReceiptOcrLayout.Bounds;
import kr.chapchap.consumption.application.service.ReceiptOcrLayout.Line;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 영수증에 나온 여러 숫자 중 실제 결제 금액으로 보이는 값을 고른다.
 *
 * 100원 이상 10억 원 이하의 숫자를 먼저 찾는다.
 * 쉼표, 원, 통화 기호가 있거나 합계나 결제금액처럼 의미가 분명한 라벨이 붙은 숫자만 금액 후보로 남긴다.
 * 같은 행에서는 라벨 앞뒤의 가까운 숫자를 보고, 행이 다르면 좌표상 같은 높이이거나 라벨 바로 아래에 있는 숫자를 연결한다.
 *
 * 결제·승인 금액, 최종 합계, 결제수단 옆 금액, 소계, 일반 금액 순으로 우선한다.
 * 세액, 공급가액, 할인, 쿠폰, 포인트, 승인번호, 날짜와 시간은 후보에서 제외한다.
 * 같은 종류의 후보끼리는 큰 금액과 반복 횟수를 작은 가점으로 사용한다.
 */
final class ReceiptAmountExtractor {

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\d{2}|(?:19|20)\\d{2})\\s*([./-])\\s*\\d{1,2}"
                    + "\\s*\\1\\s*\\d{1,2}(?!\\d)"
    );
    private static final Pattern KOREAN_DATE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:19|20)\\d{2}\\s*년\\s*\\d{1,2}\\s*월"
                    + "\\s*\\d{1,2}\\s*일"
    );
    private static final Pattern COMPACT_DATE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:19|20)\\d{6}(?!\\d)"
    );
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?<!\\d)(?:[01]?\\d|2[0-3])\\s*:\\s*[0-5]\\d"
                    + "(?:\\s*:\\s*[0-5]\\d)?(?!\\d)"
    );
    private static final List<Pattern> DATE_TIME_PATTERNS = List.of(
            DATE_PATTERN,
            KOREAN_DATE_PATTERN,
            COMPACT_DATE_PATTERN,
            TIME_PATTERN
    );
    private static final Pattern AMOUNT_TOKEN_PATTERN = Pattern.compile(
            "(?<![\\d-])(?<currency>[₩￦])?\\s*"
                    + "(?<number>\\d{1,3}(?:\\s*[,．.]\\s*\\d{3})+|\\d{3,10})"
                    + "\\s*(?<won>원)?(?![\\d-])"
    );
    private static final Pattern ITEM_SECTION_PATTERN = Pattern.compile(
            ".*(?:메뉴|상품|품명|제품명|item|product|qty).*"
    );
    private static final Pattern EXCLUDED_AMOUNT_COMPOUND_PATTERN = Pattern.compile(
            "(?:할인|쿠폰|포인트|세액|부가세|과세|면세|공급가(?:액)?|tax|vat)"
                    + "(?:사용|적립|적용|차감|사용액)?(?:금액|합계|총계|총액|total)"
    );
    private static final Pattern SETTLED_AMOUNT_COMPOUND_PATTERN = Pattern.compile(
            "(?:최종(?:결제)?금액|결제(?:대상|완료|승인|청구)?금액|실(?:제)?결제금액)"
    );

    private static final List<AmountLabelGroup> POSITIVE_AMOUNT_LABEL_GROUPS = List.of(
            new AmountLabelGroup(List.of(
                    new WeightedLabel("최종결제금액", 135),
                    new WeightedLabel("실결제금액", 135),
                    new WeightedLabel("카드청구액", 130),
                    new WeightedLabel("카드결제액", 130),
                    new WeightedLabel("승인금액", 130),
                    new WeightedLabel("결제금액", 130),
                    new WeightedLabel("청구금액", 125),
                    new WeightedLabel("결제액", 125),
                    new WeightedLabel("카드결제", 95),
                    new WeightedLabel("현금결제", 95)
            ), AmountKind.SETTLED_PAYMENT),
            new AmountLabelGroup(List.of(
                    new WeightedLabel("받을금액", 110),
                    new WeightedLabel("합계금액", 105),
                    new WeightedLabel("총합계", 105),
                    new WeightedLabel("grandtotal", 105),
                    new WeightedLabel("total", 100),
                    new WeightedLabel("총액", 100),
                    new WeightedLabel("합계", 105)
            ), AmountKind.FINAL_TOTAL),
            new AmountLabelGroup(List.of(
                    new WeightedLabel("판매계", 100),
                    new WeightedLabel("subtotal", 60),
                    new WeightedLabel("판매금액", 55),
                    new WeightedLabel("상품합계", 55),
                    new WeightedLabel("주문금액", 55),
                    new WeightedLabel("소계", 45)
            ), AmountKind.SUBTOTAL),
            new AmountLabelGroup(
                    List.of(new WeightedLabel("금액", 35)),
                    AmountKind.GENERIC
            )
    );
    private static final List<WeightedLabel> EXCLUDED_AMOUNT_LABELS = List.of(
            new WeightedLabel("사업자번호", 150),
            new WeightedLabel("승인번호", 150),
            new WeightedLabel("승인no", 150),
            new WeightedLabel("카드번호", 150),
            new WeightedLabel("거래번호", 150),
            new WeightedLabel("영수번호", 150),
            new WeightedLabel("주문번호", 150),
            new WeightedLabel("가맹점no", 150),
            new WeightedLabel("가맹번호", 150),
            new WeightedLabel("일련번호", 150),
            new WeightedLabel("처리번호", 150),
            new WeightedLabel("catid", 150),
            new WeightedLabel("tid", 150),
            new WeightedLabel("전화번호", 150),
            new WeightedLabel("회원번호", 140),
            new WeightedLabel("할인합계", 135),
            new WeightedLabel("부가세합계", 135),
            new WeightedLabel("과세합계", 135),
            new WeightedLabel("면세합계", 135),
            new WeightedLabel("공급가합계", 135),
            new WeightedLabel("세액합계", 135),
            new WeightedLabel("받은금액", 135),
            new WeightedLabel("현금수령", 130),
            new WeightedLabel("거스름돈", 130),
            new WeightedLabel("과세물품가액", 125),
            new WeightedLabel("면세물품가액", 125),
            new WeightedLabel("공급가액", 120),
            new WeightedLabel("부가세", 120),
            new WeightedLabel("할인금액", 115),
            new WeightedLabel("예수금", 110),
            new WeightedLabel("포인트", 110),
            new WeightedLabel("쿠폰", 110),
            new WeightedLabel("거스름", 110),
            new WeightedLabel("잔액", 105),
            new WeightedLabel("단가", 100),
            new WeightedLabel("수량", 100),
            new WeightedLabel("할인", 95),
            new WeightedLabel("세액", 90),
            new WeightedLabel("과세", 80),
            new WeightedLabel("면세", 80),
            new WeightedLabel("vat", 120)
    );

    private static final double AMOUNT_MINIMUM_SCORE = 30.0;
    private static final long MINIMUM_REASONABLE_AMOUNT = 100L;
    private static final long MAXIMUM_REASONABLE_AMOUNT = 1_000_000_000L;

    Long extract(ReceiptOcrLayout layout) {
        List<Line> lines = layout.lines();
        List<LabelHit> labelHits = collectAmountLabelHits(lines);
        List<AmountCandidate> candidates = new ArrayList<>();

        for (Line line : lines) {
            Matcher matcher = AMOUNT_TOKEN_PATTERN.matcher(line.text());
            while (matcher.find()) {
                Long value = parseAmount(matcher.group("number"));
                if (value == null
                        || value < MINIMUM_REASONABLE_AMOUNT
                        || value > MAXIMUM_REASONABLE_AMOUNT) {
                    continue;
                }

                boolean formatted = matcher.group("currency") != null
                        || matcher.group("won") != null
                        || matcher.group("number").matches(".*[,．.].*");
                Bounds bounds = line.boundsForRange(matcher.start(), matcher.end());
                AmountToken token = new AmountToken(
                        value,
                        line,
                        matcher.start(),
                        matcher.end(),
                        bounds,
                        line.confidenceForRange(matcher.start(), matcher.end()),
                        formatted
                );
                AmountCandidate candidate = scoreAmountCandidate(token, labelHits, layout);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        long maximumValue = candidates.stream()
                .mapToLong(candidate -> candidate.token().value())
                .max()
                .orElse(0L);
        // 같은 금액의 반복 출현은 보조 근거일 뿐 라벨의 의미보다 우선하지 않는다.
        Map<Long, List<AmountCandidate>> candidatesByValue = new HashMap<>();
        candidates.forEach(candidate -> candidatesByValue
                .computeIfAbsent(candidate.token().value(), ignored -> new ArrayList<>())
                .add(candidate));

        List<AmountValueCandidate> scoredValues = new ArrayList<>();
        for (Map.Entry<Long, List<AmountCandidate>> entry : candidatesByValue.entrySet()) {
            AmountCandidate bestCandidate = entry.getValue().stream()
                    .max(amountCandidateComparator())
                    .orElseThrow();
            double score = bestCandidate.score();
            score += Math.min(6.0, Math.max(0, entry.getValue().size() - 1) * 2.0);
            if (entry.getKey() == maximumValue) {
                score += 2.0;
            }
            scoredValues.add(new AmountValueCandidate(
                    entry.getKey(),
                    bestCandidate.kind(),
                    score,
                    bestCandidate.token().line().index(),
                    layout.verticalRatio(bestCandidate.token().line()),
                    bestCandidate.token().confidence()
            ));
        }
        // 숫자의 크기보다 라벨이 나타내는 금액 종류를 먼저 비교한다.
        return scoredValues.stream()
                .filter(candidate -> candidate.kind() != AmountKind.UNLABELED
                        || candidate.score() >= AMOUNT_MINIMUM_SCORE)
                .max(Comparator.comparingInt(
                                (AmountValueCandidate candidate) -> candidate.kind().priority()
                        )
                        .thenComparingDouble(AmountValueCandidate::score)
                        .thenComparingDouble(AmountValueCandidate::confidence)
                        .thenComparingDouble(AmountValueCandidate::verticalRatio)
                        .thenComparingInt(AmountValueCandidate::lineIndex))
                .map(AmountValueCandidate::value)
                .orElse(null);
    }

    // 같은 행의 가까운 라벨을 먼저 연결하고, 없을 때만 좌표상 인접 라벨을 사용한다.
    private AmountCandidate scoreAmountCandidate(
            AmountToken token,
            List<LabelHit> labelHits,
            ReceiptOcrLayout layout
    ) {
        if (overlapsDateOrTime(token)) {
            return null;
        }
        List<LabelHit> sameLineHits = labelHits.stream()
                .filter(hit -> hit.line().index() == token.line().index())
                .toList();
        LabelHit nearestLocalHit = nearestLocalLabelHit(token, sameLineHits);

        double semanticScore = 0.0;
        AmountKind kind = AmountKind.UNLABELED;
        if (nearestLocalHit != null && characterDistance(token, nearestLocalHit) <= 24) {
            if (nearestLocalHit.kind() == AmountKind.EXCLUDED) {
                return null;
            }
            kind = nearestLocalHit.kind();
            semanticScore = nearestLocalHit.weight()
                    + (nearestLocalHit.endOffset() <= token.startOffset() ? 25.0 : 10.0);
        } else {
            LabelAssociation association = bestSpatialAssociation(token, labelHits, layout);
            if (association != null) {
                if (association.kind() == AmountKind.EXCLUDED) {
                    return null;
                }
                kind = association.kind();
                semanticScore = association.score();
            }
        }

        if (!token.formatted()
                && (kind == AmountKind.UNLABELED || kind == AmountKind.PAYMENT_METHOD)) {
            return null;
        }

        double score = semanticScore;
        score += token.formatted() ? 18.0 : 0.0;
        score += token.confidence() * 6.0;
        score += layout.verticalRatio(token.line()) * 5.0;
        return new AmountCandidate(token, kind, score);
    }

    private boolean overlapsDateOrTime(AmountToken token) {
        return DATE_TIME_PATTERNS.stream().anyMatch(pattern -> overlapsPattern(token, pattern));
    }

    private boolean overlapsPattern(AmountToken token, Pattern pattern) {
        Matcher matcher = pattern.matcher(token.line().text());
        while (matcher.find()) {
            if (matcher.end() > token.startOffset() && matcher.start() < token.endOffset()) {
                return true;
            }
        }
        return false;
    }

    private LabelHit nearestLocalLabelHit(AmountToken token, List<LabelHit> hits) {
        Comparator<LabelHit> comparator = Comparator
                .comparingInt((LabelHit hit) -> characterDistance(token, hit))
                .thenComparing(Comparator.comparingInt(LabelHit::length).reversed())
                .thenComparing(Comparator.comparingInt(LabelHit::weight).reversed());

        LabelHit precedingHit = hits.stream()
                .filter(hit -> hit.endOffset() <= token.startOffset())
                .filter(hit -> characterDistance(token, hit) <= 24)
                .min(comparator)
                .orElse(null);
        if (precedingHit != null) {
            return precedingHit;
        }
        return hits.stream()
                .filter(hit -> token.endOffset() <= hit.startOffset())
                .filter(hit -> characterDistance(token, hit) <= 24)
                .min(comparator)
                .orElse(null);
    }

    private LabelAssociation bestSpatialAssociation(
            AmountToken token,
            List<LabelHit> labelHits,
            ReceiptOcrLayout layout
    ) {
        List<LabelAssociation> associations = new ArrayList<>();
        for (LabelHit hit : labelHits) {
            if (hit.line().index() == token.line().index()) {
                continue;
            }

            boolean sameRow = layout.isSameRow(hit.bounds(), token.bounds());
            double relationScore;
            if (sameRow) {
                relationScore = 38.0;
                if (hit.bounds().isPresent()
                        && token.bounds().isPresent()
                        && token.bounds().left() >= hit.bounds().left()) {
                    relationScore += 10.0;
                }
            } else if (layout.isImmediatelyBelow(hit.line(), token.line())
                    || (!hit.bounds().isPresent()
                    && token.line().index() == hit.line().index() + 1)) {
                relationScore = 22.0;
            } else {
                continue;
            }

            double distance = spatialDistance(
                    hit.bounds(),
                    token.bounds(),
                    hit.line(),
                    token.line()
            );
            if (sameRow
                    && hit.bounds().isPresent()
                    && token.bounds().isPresent()
                    && hit.bounds().left() > token.bounds().right()) {
                distance += hit.kind() == AmountKind.EXCLUDED ? 15.0 : 5.0;
            }
            if (distance > 25.0) {
                continue;
            }
            associations.add(new LabelAssociation(
                    hit.kind(),
                    hit.weight() + relationScore,
                    distance
            ));
        }
        return associations.stream()
                .min(Comparator.comparingDouble(LabelAssociation::distance)
                        .thenComparingInt(association -> association.kind().priority())
                        .thenComparing(Comparator.comparingDouble(
                                LabelAssociation::score
                        ).reversed()))
                .orElse(null);
    }

    private double spatialDistance(
            Bounds labelBounds,
            Bounds tokenBounds,
            Line labelLine,
            Line tokenLine
    ) {
        if (labelBounds.isPresent() && tokenBounds.isPresent()) {
            double referenceHeight = Math.max(
                    1.0,
                    Math.min(labelBounds.height(), tokenBounds.height())
            );
            double horizontalDistance = labelBounds.centerX() - tokenBounds.centerX();
            double verticalDistance = (labelBounds.centerY() - tokenBounds.centerY()) * 2.0;
            return Math.hypot(horizontalDistance, verticalDistance) / referenceHeight;
        }
        return Math.abs(labelLine.index() - tokenLine.index()) * 10.0;
    }

    private List<LabelHit> collectAmountLabelHits(List<Line> lines) {
        List<LabelHit> hits = new ArrayList<>();
        for (Line line : lines) {
            CompactText compactText = CompactText.from(line.text());
            for (AmountLabelGroup group : POSITIVE_AMOUNT_LABEL_GROUPS) {
                collectExactLabelHits(
                        hits,
                        line,
                        compactText,
                        group.labels(),
                        group.kind()
                );
            }
            collectExactLabelHits(
                    hits,
                    line,
                    compactText,
                    EXCLUDED_AMOUNT_LABELS,
                    AmountKind.EXCLUDED
            );
            collectCompoundLabelHits(
                    hits,
                    line,
                    compactText,
                    EXCLUDED_AMOUNT_COMPOUND_PATTERN,
                    145,
                    AmountKind.EXCLUDED
            );
            collectCompoundLabelHits(
                    hits,
                    line,
                    compactText,
                    SETTLED_AMOUNT_COMPOUND_PATTERN,
                    145,
                    AmountKind.SETTLED_PAYMENT
            );

            String normalizedLine = compactText.value();
            if (line.text().matches(".*\\[\\s*금액\\s*].*")
                    && isCardSlipAmountLine(lines, line)) {
                int index = normalizedLine.indexOf("금액");
                if (index >= 0) {
                    hits.add(labelHit(
                            line,
                            compactText,
                            index,
                            index + 2,
                            140,
                            AmountKind.SETTLED_PAYMENT
                    ));
                }
            }
            collectPaymentMethodLabelHit(hits, line, compactText);
            if (normalizedLine.equals("계")) {
                hits.add(labelHit(
                        line,
                        compactText,
                        0,
                        1,
                        80,
                        AmountKind.FINAL_TOTAL
                ));
            }
            collectFuzzyAmountLabelHit(hits, line, compactText);
        }

        return hits.stream()
                .filter(hit -> hits.stream().noneMatch(other -> other != hit
                        && other.line().index() == hit.line().index()
                        && other.startOffset() <= hit.startOffset()
                        && other.endOffset() >= hit.endOffset()
                        && other.length() > hit.length()))
                .toList();
    }

    private void collectCompoundLabelHits(
            List<LabelHit> hits,
            Line line,
            CompactText compactText,
            Pattern pattern,
            int weight,
            AmountKind kind
    ) {
        Matcher matcher = pattern.matcher(compactText.value());
        while (matcher.find()) {
            hits.add(labelHit(
                    line,
                    compactText,
                    matcher.start(),
                    matcher.end(),
                    weight,
                    kind
            ));
        }
    }

    private boolean isCardSlipAmountLine(List<Line> lines, Line line) {
        String compactLine = compact(line.text());
        if (ITEM_SECTION_PATTERN.matcher(compactLine).matches()
                || isInsideItemSection(lines, line)) {
            return false;
        }
        if (containsAny(compactLine, List.of("일시불", "할부", "부가세", "vat"))) {
            return true;
        }

        int firstIndex = Math.max(0, line.index() - 4);
        boolean hasCardSlip = false;
        boolean hasCardIdentifier = false;
        for (int index = firstIndex; index < line.index(); index++) {
            String nearby = compact(lines.get(index).text());
            hasCardSlip |= containsAny(nearby, List.of("매출전표", "카드전표", "전자전표"));
            hasCardIdentifier |= containsAny(nearby, List.of("카드번호", "승인번호", "카드사명"));
        }
        return hasCardSlip && hasCardIdentifier;
    }

    private boolean isInsideItemSection(List<Line> lines, Line candidate) {
        int nearestHeader = -1;
        int tableHeaderSignals = 0;
        for (Line line : lines) {
            if (line.index() >= candidate.index()) {
                break;
            }
            if (ITEM_SECTION_PATTERN.matcher(compact(line.text())).matches()) {
                nearestHeader = line.index();
            }
            if (nearestHeader >= 0
                    && compact(line.text()).matches(
                    ".*(?:합계|총액|결제금액|카드청구|공급가|부가세|받을금액).*"
            )) {
                nearestHeader = -1;
            }
            if (candidate.index() - line.index() <= 6
                    && compact(line.text()).matches(
                    "(?:상품|상품명|메뉴|메뉴명|품명|제품명|수량|단가|할인|금액"
                            + "|item|product|qty)"
            )) {
                tableHeaderSignals++;
            }
        }
        return nearestHeader >= 0 || tableHeaderSignals >= 2;
    }

    private void collectPaymentMethodLabelHit(
            List<LabelHit> hits,
            Line line,
            CompactText compactText
    ) {
        String value = compactText.value();
        if (!value.matches("(?:\\d{1,2})?(?:신용카드|카드|현금|카카오페이머니|네이버페이|페이코)")) {
            return;
        }
        hits.add(labelHit(
                line,
                compactText,
                0,
                value.length(),
                100,
                AmountKind.PAYMENT_METHOD
        ));
    }

    private void collectExactLabelHits(
            List<LabelHit> hits,
            Line line,
            CompactText compactText,
            List<WeightedLabel> labels,
            AmountKind kind
    ) {
        for (WeightedLabel label : labels) {
            String labelText = compact(label.text());
            int fromIndex = 0;
            while (fromIndex < compactText.value().length()) {
                int index = compactText.value().indexOf(labelText, fromIndex);
                if (index < 0) {
                    break;
                }
                hits.add(labelHit(
                        line,
                        compactText,
                        index,
                        index + labelText.length(),
                        label.weight(),
                        kind
                ));
                fromIndex = index + 1;
            }
        }
    }

    private void collectFuzzyAmountLabelHit(
            List<LabelHit> hits,
            Line line,
            CompactText compactText
    ) {
        if (compactText.value().length() < 4 || compactText.value().length() > 8) {
            return;
        }
        for (AmountLabelGroup group : POSITIVE_AMOUNT_LABEL_GROUPS) {
            for (WeightedLabel label : group.labels()) {
                String labelText = compact(label.text());
                if (labelText.length() >= 4
                        && isEditDistanceAtMostOne(compactText.value(), labelText)) {
                    hits.add(labelHit(
                            line,
                            compactText,
                            0,
                            compactText.value().length(),
                            Math.max(40, label.weight() - 20),
                            group.kind()
                    ));
                    return;
                }
            }
        }
    }

    private LabelHit labelHit(
            Line line,
            CompactText compactText,
            int compactStart,
            int compactEnd,
            int weight,
            AmountKind kind
    ) {
        int startOffset = compactText.originalOffset(compactStart);
        int endOffset = compactText.originalEndOffset(compactEnd - 1);
        return new LabelHit(
                line,
                startOffset,
                endOffset,
                weight,
                kind,
                line.boundsForRange(startOffset, endOffset)
        );
    }

    private int characterDistance(AmountToken token, LabelHit hit) {
        if (hit.endOffset() <= token.startOffset()) {
            return token.startOffset() - hit.endOffset();
        }
        if (token.endOffset() <= hit.startOffset()) {
            return hit.startOffset() - token.endOffset();
        }
        return 0;
    }

    private Comparator<AmountCandidate> amountCandidateComparator() {
        return Comparator.comparingInt(
                        (AmountCandidate candidate) -> candidate.kind().priority()
                )
                .thenComparingDouble(AmountCandidate::score)
                .thenComparingDouble(candidate -> candidate.token().confidence())
                .thenComparingInt(candidate -> candidate.token().line().index());
    }

    private Long parseAmount(String value) {
        try {
            long amount = Long.parseLong(value.replaceAll("[,．.\\s]", ""));
            return amount > 0 ? amount : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean containsAny(String compactText, List<String> values) {
        return values.stream()
                .map(ReceiptAmountExtractor::compact)
                .anyMatch(compactText::contains);
    }

    private static String compact(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private boolean isEditDistanceAtMostOne(String first, String second) {
        if (Math.abs(first.length() - second.length()) > 1) {
            return false;
        }
        int firstIndex = 0;
        int secondIndex = 0;
        int edits = 0;
        while (firstIndex < first.length() && secondIndex < second.length()) {
            if (first.charAt(firstIndex) == second.charAt(secondIndex)) {
                firstIndex++;
                secondIndex++;
                continue;
            }
            if (++edits > 1) {
                return false;
            }
            if (first.length() > second.length()) {
                firstIndex++;
            } else if (first.length() < second.length()) {
                secondIndex++;
            } else {
                firstIndex++;
                secondIndex++;
            }
        }
        if (firstIndex < first.length() || secondIndex < second.length()) {
            edits++;
        }
        return edits <= 1;
    }

    private record WeightedLabel(String text, int weight) {
    }

    private record AmountLabelGroup(List<WeightedLabel> labels, AmountKind kind) {

        private AmountLabelGroup {
            labels = List.copyOf(labels);
        }
    }

    private enum AmountKind {
        EXCLUDED(-1),
        UNLABELED(0),
        GENERIC(1),
        SUBTOTAL(2),
        PAYMENT_METHOD(3),
        FINAL_TOTAL(4),
        SETTLED_PAYMENT(5);

        private final int priority;

        AmountKind(int priority) {
            this.priority = priority;
        }

        int priority() {
            return priority;
        }
    }

    private record AmountToken(
            long value,
            Line line,
            int startOffset,
            int endOffset,
            Bounds bounds,
            double confidence,
            boolean formatted
    ) {
    }

    private record AmountCandidate(AmountToken token, AmountKind kind, double score) {
    }

    private record LabelHit(
            Line line,
            int startOffset,
            int endOffset,
            int weight,
            AmountKind kind,
            Bounds bounds
    ) {

        int length() {
            return endOffset - startOffset;
        }
    }

    private record LabelAssociation(AmountKind kind, double score, double distance) {
    }

    private record AmountValueCandidate(
            long value,
            AmountKind kind,
            double score,
            int lineIndex,
            double verticalRatio,
            double confidence
    ) {
    }

    private record CompactText(String value, List<Integer> originalOffsets) {

        static CompactText from(String source) {
            StringBuilder compact = new StringBuilder();
            List<Integer> offsets = new ArrayList<>();
            for (int index = 0; index < source.length(); index++) {
                char character = source.charAt(index);
                if (Character.isWhitespace(character)
                        || "[]{}()_:：=".indexOf(character) >= 0) {
                    continue;
                }
                compact.append(Character.toLowerCase(character));
                offsets.add(index);
            }
            return new CompactText(compact.toString(), List.copyOf(offsets));
        }

        int originalOffset(int compactIndex) {
            if (originalOffsets.isEmpty()) {
                return 0;
            }
            int safeIndex = Math.max(0, Math.min(compactIndex, originalOffsets.size() - 1));
            return originalOffsets.get(safeIndex);
        }

        int originalEndOffset(int compactIndex) {
            return originalOffset(compactIndex) + 1;
        }
    }
}
