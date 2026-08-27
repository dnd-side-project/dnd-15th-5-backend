package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.service.ReceiptOcrLayout.Bounds;
import kr.chapchap.consumption.application.service.ReceiptOcrLayout.Line;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR 결과에서 상호명 후보를 모아 가장 가능성이 높은 값을 고른다.
 *
 * 매장명, 구매매장, 상호처럼 라벨이 붙은 값을 먼저 확인한다.
 * 영수증 형식이 확인되면 라벨 후보와 별개로 위쪽의 로고나 지점명, 주변 사업자 정보와 주소도 같이 본다.
 * 영수증 제목, 안내 문구, 상품명, 담당자, 주소, 번호, 결제대행사 이름은 후보에서 제외한다.
 *
 * 남은 후보의 점수도 기준에 못 미치면 잘못된 이름을 채우지 않고 null을 반환한다.
 */
final class ReceiptStoreExtractor {

    private static final String STORE_LABEL = "(?:주문\\s*매장|구매\\s*매장|매장\\s*명"
            + "|가맹점\\s*명|상호\\s*명?|점포\\s*명|지점\\s*명|매장)";
    private static final Pattern STORE_LABEL_PATTERN = Pattern.compile(
            "^\\s*(?:\\[\\s*)?(?<label>" + STORE_LABEL + ")"
                    + "(?:\\s*\\])?(?:(?:\\s*[:：=]\\s*|\\s+)(?<value>.*))?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REGION_PATTERN = Pattern.compile(
            "(?:서울(?:특별시|시)?|부산(?:광역시|시)?|대구(?:광역시|시)?"
                    + "|인천(?:광역시|시)?|광주(?:광역시|시)?|대전(?:광역시|시)?"
                    + "|울산(?:광역시|시)?|세종(?:특별자치시|시)?|경기(?:도)?"
                    + "|강원(?:특별자치도|도)?|충청[남북]도|충[남북]|전라[남북]도"
                    + "|전[남북]|경상[남북]도|경[남북]|제주(?:특별자치도|도)?"
                    + "|[가-힣]{2,}(?:특별시|광역시|특별자치시|특별자치도|도))"
    );
    private static final Pattern DISTRICT_PATTERN = Pattern.compile(
            "[가-힣]{2,}(?:시|군|구)(?=\\s|[가-힣0-9])"
    );
    private static final Pattern NON_ADDRESS_DISTRICT_PATTERN = Pattern.compile(
            ".*(?:구매|구입|방문|환불|교환|반품|결제|사용|이용|주문|적립|참여"
                    + "|신청|문의|요청|수령|발급|제시|보관|지참|취소|일)시$"
    );
    private static final Pattern ROAD_ADDRESS_PATTERN = Pattern.compile(
            "[가-힣0-9·.-]{2,}(?:대로|로|길)[가-힣0-9-]*\\s*[,]?\\s*\\d+(?:-\\d+)?"
    );
    private static final Pattern LOT_NUMBER_ADDRESS_PATTERN = Pattern.compile(
            "[가-힣0-9·.-]{2,}(?:동|읍|면|리)\\s*\\d+(?:-\\d+)?"
    );
    private static final Pattern DAMAGED_ADDRESS_PATTERN = Pattern.compile(
            "[가-힣]{2,}(?:시|군|구)\\s+[가-힣0-9·.-]{2,}\\s+\\d+(?:-\\d+)?"
    );
    private static final Pattern ADDRESS_UNIT_PATTERN = Pattern.compile(
            "(?:\\d+\\s*)?(?:층|호|동|관)\\b|(?:타워|빌딩|몰|백화점)"
    );
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[가-힣A-Za-z].*");
    private static final Pattern STORE_SENTENCE_PATTERN = Pattern.compile(
            ".*(?:합니다|됩니다|경우|불가|바랍니다|지참|감사합니다|죄송|주세요|이에요)[.! ]*$"
    );
    private static final Pattern STORE_NUMBER_LABEL_PATTERN = Pattern.compile(
            "(?i).*(?:번호|no\\.?|pos|bill)\\s*[:：]?.*\\d.*"
    );
    private static final Pattern BUSINESS_REGISTRATION_NUMBER_PATTERN = Pattern.compile(
            ".*\\d{3}-\\d{2}-\\d{5}.*"
    );
    private static final Pattern ITEM_SECTION_PATTERN = Pattern.compile(
            ".*(?:메뉴|상품|품명|제품명|item|product|qty).*"
    );
    private static final Pattern STORE_DOCUMENT_HEADER_PATTERN = Pattern.compile(
            "(?:(?:original|duplicate|customer|merchant|creditcard|card|sales|tax|payment)*"
                    + "(?:receipt|invoice|copy)|thankyou(?:for.*)?)"
    );
    private static final Pattern STORE_GENERIC_BRANCH_PATTERN = Pattern.compile(
            ".*(?:사용|이용|적립|방문|교환|환불|판매|구매)가능(?:매장|지점|점).*"
    );

    private static final Set<String> STORE_EXCLUDED_WORDS = Set.of(
            "영수증", "신용카드", "매출전표", "카드전표", "정상승인", "고객용", "사업자",
            "대표자", "전화", "주소", "주문번호", "주문접수", "거래일", "승인일", "결제일",
            "판매일", "합계", "총액", "픽업번호", "테이블명", "테이블", "재발행", "중간계산서",
            "상품명", "메뉴명", "품명", "수량", "단가", "금액", "부가세", "공급가", "할인",
            "결제내역", "회원번호", "진동벨번호", "주차정산", "주차", "제1조", "제2조",
            "교환", "환불", "문의", "안내", "주문유형", "포장주문", "매장유형", "유형",
            "판매사원", "담당자", "계산담당", "캐셔", "신고안내", "포상금", "우편접수",
            "여신금융협회", "죄송", "가맹점명", "상호명", "매장명", "점포명", "지점명"
    );
    private static final Set<String> STORE_PAYMENT_PROVIDER_WORDS = Set.of(
            "koces", "kicc", "nice", "easycheck", "ksnet", "van", "payco"
    );
    private static final Set<String> STORE_EXCLUDED_VALUES = Set.of(
            "메뉴", "상품", "세", "거래", "본점", "가맹점", "가맹번호", "신용매출",
            "매출", "매출표", "판매사원", "담당자", "계산담당자", "receipt",
            "taxinvoice", "customercopy", "merchantcopy", "cardreceipt",
            "salesreceipt", "creditcard", "approval"
    );
    private static final double STORE_MINIMUM_SCORE = 35.0;

    String extract(ReceiptOcrLayout layout) {
        List<Line> lines = layout.lines();
        List<ScoredValue<String>> candidates = new ArrayList<>();

        // 매장명처럼 의미가 명확한 라벨이 있는 후보를 가장 먼저 수집한다.
        for (Line line : lines) {
            Matcher matcher = STORE_LABEL_PATTERN.matcher(line.text());
            if (!matcher.matches()) {
                continue;
            }

            String label = compact(matcher.group("label"));
            String value = sanitizeStoreName(matcher.group("value"));
            double labelScore = storeLabelScore(label);
            double valueConfidence = matcher.group("value") == null
                    ? line.confidence()
                    : line.confidenceForRange(
                            matcher.start("value"),
                            matcher.end("value")
                    );
            if (isStoreNameCandidate(value, true)
                    && hasReliableStoreText(
                            value,
                            valueConfidence,
                            matcher.group("value") != null
                                    && line.hasConfidenceForRange(
                                            matcher.start("value"),
                                            matcher.end("value")
                                    )
                    )) {
                candidates.add(scoredStore(value, labelScore, line, layout));
                continue;
            }

            Line followingLine = nextLine(lines, line.index());
            if (followingLine != null
                    && isStoreNameCandidate(followingLine.text(), true)
                    && hasReliableStoreText(
                            followingLine.text(),
                            followingLine.confidence(),
                            followingLine.confidencePresent()
                    )
                    && isFollowingValueLine(layout, line, followingLine)) {
                candidates.add(scoredStore(
                        sanitizeStoreName(followingLine.text()),
                        labelScore - 10.0,
                        followingLine,
                        layout
                ));
            }
        }

        boolean hasReceiptEvidence = hasReceiptEvidence(lines);
        // 영수증 형식이 확인되면 로고, 지점명, 사업자 정보 주변의 후보도 함께 본다.
        if (hasReceiptEvidence) {
            for (Line line : lines) {
                String value = sanitizeStoreName(line.text());
                if (!isStoreNameCandidate(value, false)) {
                    continue;
                }
                if (layout.verticalRatio(line) > 0.33
                        && !containsBranchName(value)
                        && !isFollowingReceiptHeader(lines, line)) {
                    continue;
                }
                if (!hasReliableStoreText(
                        value,
                        line.confidence(),
                        line.confidencePresent()
                )) {
                    continue;
                }
                if (isMetadataValueLine(lines, line)) {
                    continue;
                }
                if (!hasStrongStoreEvidence(layout, lines, line, value)) {
                    continue;
                }

                double score = 24.0;
                score += Math.max(0.0, 20.0 * (1.0 - layout.verticalRatio(line)));
                score += line.confidence() * 5.0;
                if (containsBranchName(value)) {
                    score += 26.0;
                }
                if (containsCompanyMarker(value)) {
                    score += 7.0;
                }
                if (hasNearbyBusinessEvidence(lines, line)) {
                    score += 12.0;
                }
                if (isFollowingReceiptHeader(lines, line)) {
                    score += 30.0;
                }
                if (isShortUppercaseLogo(value)) {
                    score -= 16.0;
                }
                if (isInsideItemSection(lines, line)) {
                    score -= 45.0;
                }
                candidates.add(new ScoredValue<>(
                        value,
                        score,
                        line.index(),
                        layout.verticalRatio(line),
                        line.confidence()
                ));
            }
        }

        if (hasReceiptEvidence) {
            addLogoBranchCandidates(candidates, layout);
        }

        return bestValue(
                deduplicate(candidates, ReceiptStoreExtractor::normalizeComparableValue),
                STORE_MINIMUM_SCORE
        );
    }

    private ScoredValue<String> scoredStore(
            String value,
            double baseScore,
            Line line,
            ReceiptOcrLayout layout
    ) {
        double score = baseScore + line.confidence() * 5.0;
        if (containsBranchName(value)) {
            score += 15.0;
        }
        return new ScoredValue<>(
                value,
                score,
                line.index(),
                layout.verticalRatio(line),
                line.confidence()
        );
    }

    private double storeLabelScore(String label) {
        if (label.contains("주문매장") || label.contains("구매매장")) {
            return 155.0;
        }
        if (label.contains("매장명")
                || label.contains("가맹점명")
                || label.contains("상호")
                || label.contains("점포명")
                || label.contains("지점명")) {
            return 145.0;
        }
        return 135.0;
    }

    private boolean isStoreNameCandidate(String value, boolean labeled) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim();
        String compactValue = compact(normalized.toLowerCase(Locale.ROOT));
        int minimumLength = labeled ? 1 : 2;
        if (normalized.length() < minimumLength
                || normalized.length() > 60
                || !LETTER_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        if (STORE_EXCLUDED_WORDS.stream().anyMatch(compactValue::contains)
                || isPaymentProviderText(compactValue)
                || STORE_EXCLUDED_VALUES.contains(compactValue)) {
            return false;
        }
        if (STORE_SENTENCE_PATTERN.matcher(normalized).matches()
                || STORE_DOCUMENT_HEADER_PATTERN.matcher(compactValue).matches()
                || STORE_GENERIC_BRANCH_PATTERN.matcher(compactValue).matches()
                || STORE_NUMBER_LABEL_PATTERN.matcher(normalized).matches()
                || (!labeled && normalized.matches(".*[:：].*"))
                || normalized.matches("^\\[?\\s*(?:\\d+|[A-Z가-힣])\\s*홀\\s*\\]?$")
                || normalized.matches("^\\d+(?:[.,]\\d+)?(?:점|개|명|원|층|호|번|회)$")
                || normalized.matches("^제\\s*\\d+\\s*조.*$")) {
            return false;
        }
        if (looksLikeAddress(normalized)
                || (ADDRESS_UNIT_PATTERN.matcher(normalized).find()
                && normalized.chars().anyMatch(Character::isDigit))
                || ReceiptDateTimeExtractor.findDateInLine(normalized) != null
                || ReceiptDateTimeExtractor.findTimeInLine(normalized) != null
                || normalized.matches(".*https?://.*|.*www\\..*|.*@.*")) {
            return false;
        }

        long digitCount = normalized.chars().filter(Character::isDigit).count();
        return digitCount <= Math.max(2, normalized.length() / 3);
    }

    private boolean isPaymentProviderText(String compactValue) {
        if (STORE_PAYMENT_PROVIDER_WORDS.contains(compactValue)) {
            return true;
        }
        if (compactValue.matches("(?:(?:koces|kicc|nice|easycheck|ksnet|van|payco))+")) {
            return true;
        }
        return STORE_PAYMENT_PROVIDER_WORDS.stream().anyMatch(provider ->
                compactValue.startsWith(provider)
                        && compactValue.matches(
                                ".*(?:결제|카드|정보통신|승인|단말|down).*"
                        )
        );
    }

    private boolean isMetadataValueLine(List<Line> lines, Line candidate) {
        if (candidate.index() == 0) {
            return false;
        }
        String previous = compact(lines.get(candidate.index() - 1).text());
        return containsAny(previous, List.of(
                "대표자", "매입사", "판매사원", "담당자", "계산담당", "회원", "카드",
                "사업자", "가맹번호", "전화번호"
        ));
    }

    // 라벨 없는 문자열은 지점명·로고·사업자 정보 중 하나가 있어야 상호명 후보가 된다.
    private boolean hasStrongStoreEvidence(
            ReceiptOcrLayout layout,
            List<Line> lines,
            Line candidate,
            String value
    ) {
        return containsBranchName(value)
                || containsCompanyMarker(value)
                || isTopLatinLogo(layout, candidate, value)
                || isFollowingReceiptHeader(lines, candidate)
                || hasFollowingBusinessIdentity(lines, candidate);
    }

    private boolean isFollowingReceiptHeader(List<Line> lines, Line candidate) {
        if (candidate.index() == 0) {
            return false;
        }
        String previous = compact(lines.get(candidate.index() - 1).text());
        return Set.of("영수증", "판매영수증", "카드판매영수증").contains(previous);
    }

    private boolean isTopLatinLogo(
            ReceiptOcrLayout layout,
            Line candidate,
            String value
    ) {
        if (layout.verticalRatio(candidate) > 0.2
                || !value.matches("[A-Za-z][A-Za-z .&'-]{2,30}")) {
            return false;
        }
        return value.chars().filter(Character::isLetter).count() >= 3;
    }

    private boolean hasFollowingBusinessIdentity(List<Line> lines, Line candidate) {
        int lastIndex = Math.min(lines.size() - 1, candidate.index() + 4);
        for (int index = candidate.index() + 1; index <= lastIndex; index++) {
            String text = lines.get(index).text();
            String compactText = compact(text);
            if (containsAny(compactText, List.of("사업자", "대표", "전화", "tel", "주소"))
                    || BUSINESS_REGISTRATION_NUMBER_PATTERN.matcher(text).matches()
                    || looksLikeAddress(text)) {
                return true;
            }
        }
        return false;
    }

    private String sanitizeStoreName(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceFirst(
                        "(?i)^\\s*\\[\\s*(?:판매\\s*)?영수증\\s*\\]\\s*",
                        ""
                )
                .replaceFirst("(?i)^\\s*(?:판매\\s*)?영수증\\s*", "")
                .replaceFirst("^[\\s:：=,.-]+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasReliableStoreText(
            String value,
            double confidence,
            boolean hasOcrMetadata
    ) {
        if (!hasOcrMetadata) {
            return true;
        }
        long letterCount = value.codePoints()
                .filter(Character::isLetter)
                .count();
        if (letterCount <= 1 && confidence < 0.75) {
            return false;
        }
        return confidence >= 0.2;
    }

    private void addLogoBranchCandidates(
            List<ScoredValue<String>> candidates,
            ReceiptOcrLayout layout
    ) {
        LogoCandidate logo = findTopUppercaseLogo(layout);
        if (logo == null) {
            return;
        }

        for (Line line : layout.lines()) {
            String branch = sanitizeStoreName(line.text());
            if (layout.verticalRatio(line) > 0.35
                    || !containsBranchName(branch)
                    || containsCompanyMarker(branch)
                    || !isStoreNameCandidate(branch, false)
                    || compact(branch).contains(compact(logo.value()))
                    || line.index() - logo.line().index() > 5) {
                continue;
            }
            candidates.add(new ScoredValue<>(
                    logo.value() + " " + branch,
                    115.0 + line.confidence() * 5.0,
                    line.index(),
                    layout.verticalRatio(line),
                    line.confidence()
            ));
        }
    }

    private LogoCandidate findTopUppercaseLogo(ReceiptOcrLayout layout) {
        List<Line> lines = layout.lines();
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (layout.verticalRatio(line) > 0.2) {
                break;
            }
            String first = uppercaseLogoText(line.text());
            if (first == null) {
                continue;
            }

            Line nextLine = nextLine(lines, index);
            if (nextLine != null) {
                String second = uppercaseLogoText(nextLine.text());
                if (second != null && areVerticallyAligned(line.bounds(), nextLine.bounds())) {
                    return new LogoCandidate(
                            first.equals(second) ? first : first + second,
                            nextLine
                    );
                }
            }
            return new LogoCandidate(first, line);
        }
        return null;
    }

    private String uppercaseLogoText(String value) {
        String normalized = sanitizeStoreName(value).replaceAll("\\s+", "");
        if (!normalized.matches("[A-Z][A-Z&.'-]{1,19}")) {
            return null;
        }
        String lowerCase = normalized.toLowerCase(Locale.ROOT);
        if (STORE_DOCUMENT_HEADER_PATTERN.matcher(lowerCase).matches()) {
            return null;
        }
        if (Set.of(
                "tel", "pos", "bill", "no", "receipt", "total", "card", "van", "tax",
                "approval", "cashier", "wifi", "id", "pw", "koces", "kicc", "nice",
                "easycheck", "ksnet", "payco"
        ).contains(lowerCase)) {
            return null;
        }
        return normalized;
    }

    private boolean areVerticallyAligned(Bounds first, Bounds second) {
        if (!first.isPresent() || !second.isPresent()) {
            return true;
        }
        double referenceWidth = Math.max(first.width(), second.width());
        return second.centerY() > first.centerY()
                && Math.abs(first.centerX() - second.centerX()) <= referenceWidth * 0.35;
    }

    private boolean containsBranchName(String value) {
        String compactValue = compact(value);
        return compactValue.matches(".*(?:지점|본점|직영점|센터점|몰점|역점|점)$")
                || compactValue.matches(".*\\([^)]*점\\).*");
    }

    private boolean containsCompanyMarker(String value) {
        String compactValue = compact(value);
        return value.contains("(주)")
                || value.contains("㈜")
                || compactValue.contains("주식회사");
    }

    private boolean isShortUppercaseLogo(String value) {
        if (!value.matches("[A-Z][A-Z .&'-]*")) {
            return false;
        }
        String lettersOnly = value.replaceAll("[^A-Za-z]", "");
        return !lettersOnly.isEmpty()
                && lettersOnly.length() <= 4
                && lettersOnly.equals(lettersOnly.toUpperCase(Locale.ROOT));
    }

    private boolean hasNearbyBusinessEvidence(List<Line> lines, Line candidate) {
        return lines.stream()
                .filter(line -> Math.abs(line.index() - candidate.index()) <= 4)
                .map(Line::text)
                .map(ReceiptStoreExtractor::compact)
                .anyMatch(text -> text.contains("사업자")
                        || text.contains("대표")
                        || text.contains("전화")
                        || text.contains("tel")
                        || text.contains("주소"));
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

    private boolean hasReceiptEvidence(List<Line> lines) {
        if (lines.stream()
                .map(Line::text)
                .map(ReceiptStoreExtractor::compact)
                .anyMatch(value -> Set.of(
                        "영수증", "판매영수증", "카드판매영수증"
                ).contains(value))) {
            return true;
        }
        int evidence = 0;
        if (lines.stream().anyMatch(line ->
                ReceiptDateTimeExtractor.findDateInLine(line.text()) != null
                        || ReceiptDateTimeExtractor.findTimeInLine(line.text()) != null)) {
            evidence++;
        }
        if (lines.stream().map(Line::text).map(ReceiptStoreExtractor::compact)
                .anyMatch(text -> containsAny(text, List.of(
                        "합계", "결제", "승인", "금액", "total", "부가세", "공급가"
                )))) {
            evidence++;
        }
        if (lines.stream().map(Line::text).map(ReceiptStoreExtractor::compact)
                .anyMatch(text -> containsAny(text, List.of(
                        "사업자", "대표", "전화", "tel", "주소", "카드", "영수증", "매출전표"
                )))) {
            evidence++;
        }
        return evidence >= 2;
    }

    private boolean looksLikeAddress(String value) {
        boolean hasRegion = REGION_PATTERN.matcher(value).find();
        boolean hasLocation = ROAD_ADDRESS_PATTERN.matcher(value).find()
                || LOT_NUMBER_ADDRESS_PATTERN.matcher(value).find()
                || DAMAGED_ADDRESS_PATTERN.matcher(value).find();
        return (hasRegion || hasValidDistrict(value)) && hasLocation;
    }

    private boolean hasValidDistrict(String value) {
        Matcher matcher = DISTRICT_PATTERN.matcher(value);
        while (matcher.find()) {
            if (!NON_ADDRESS_DISTRICT_PATTERN.matcher(compact(matcher.group())).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean isFollowingValueLine(ReceiptOcrLayout layout, Line label, Line value) {
        return !label.bounds().isPresent()
                || !value.bounds().isPresent()
                || layout.isImmediatelyBelow(label, value)
                || layout.isSameRow(label, value);
    }

    private Line nextLine(List<Line> lines, int currentIndex) {
        int nextIndex = currentIndex + 1;
        return nextIndex < lines.size() ? lines.get(nextIndex) : null;
    }

    private <T> T bestValue(List<ScoredValue<T>> candidates, double minimumScore) {
        return candidates.stream()
                .filter(candidate -> candidate.score() >= minimumScore)
                .sorted(scoredValueComparator())
                .map(ScoredValue::value)
                .findFirst()
                .orElse(null);
    }

    private <T> Comparator<ScoredValue<T>> scoredValueComparator() {
        return Comparator.<ScoredValue<T>>comparingDouble(ScoredValue::score)
                .reversed()
                .thenComparing(Comparator.comparingDouble(
                        (ScoredValue<T> candidate) -> candidate.confidence()
                ).reversed())
                .thenComparingDouble(ScoredValue::verticalRatio)
                .thenComparingInt(ScoredValue::lineIndex);
    }

    private <T> List<ScoredValue<T>> deduplicate(
            List<ScoredValue<T>> candidates,
            Function<T, String> keyFunction
    ) {
        Map<String, ScoredValue<T>> unique = new LinkedHashMap<>();
        Comparator<ScoredValue<T>> comparator = scoredValueComparator();
        for (ScoredValue<T> candidate : candidates) {
            String key = keyFunction.apply(candidate.value());
            unique.merge(key, candidate, (first, second) ->
                    comparator.compare(first, second) <= 0 ? first : second
            );
        }
        return List.copyOf(unique.values());
    }

    private static String normalizeComparableValue(Object value) {
        return compact(String.valueOf(value));
    }

    private static boolean containsAny(String compactText, List<String> values) {
        return values.stream()
                .map(ReceiptStoreExtractor::compact)
                .anyMatch(compactText::contains);
    }

    private static String compact(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private record LogoCandidate(String value, Line line) {
    }

    private record ScoredValue<T>(
            T value,
            double score,
            int lineIndex,
            double verticalRatio,
            double confidence
    ) {
    }
}
