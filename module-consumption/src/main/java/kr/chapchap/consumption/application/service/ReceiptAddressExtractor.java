package kr.chapchap.consumption.application.service;

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
 * 한 줄 또는 여러 줄로 나뉜 주소를 찾아 하나의 문자열로 합친다.
 *
 * 주소나 소재지 라벨이 붙은 값을 먼저 보고, 라벨이 없으면 지역명과 도로명 또는 지번이 함께 있는 행을 찾는다.
 * 다음 행에도 층, 호, 동, 건물명 같은 주소 내용이 이어지면 최대 네 줄까지 붙인다.
 *
 * OCR 때문에 나뉜 번지수와 일부 건물명은 다시 붙이지만, 잘못 읽힌 글자를 추측해서 고치지는 않는다.
 * 주소라고 볼 근거가 부족하면 null을 반환한다.
 */
final class ReceiptAddressExtractor {

    private static final String ADDRESS_LABEL = "(?:매장\\s*주소|가맹점\\s*주소"
            + "|사업장\\s*소재지|소재지|주\\s*소|주소)";
    private static final Pattern ADDRESS_LABEL_PATTERN = Pattern.compile(
            "^\\s*(?:\\[\\s*)?(?<label>" + ADDRESS_LABEL + ")"
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
    private static final Pattern ROAD_PATTERN = Pattern.compile(
            "[가-힣0-9·.-]{2,}(?:대로|로|길)[가-힣0-9-]*\\s*[,]?\\s*\\d+(?:-\\d+)?"
    );
    private static final Pattern LOT_PATTERN = Pattern.compile(
            "[가-힣0-9·.-]{2,}(?:동|읍|면|리)\\s*\\d+(?:-\\d+)?"
    );
    private static final Pattern DAMAGED_PATTERN = Pattern.compile(
            "[가-힣]{2,}(?:시|군|구)\\s+[가-힣0-9·.-]{2,}\\s+\\d+(?:-\\d+)?"
    );
    private static final Pattern ADDRESS_UNIT_PATTERN = Pattern.compile(
            "(?:\\d+\\s*)?(?:층|호|동|관)\\b|(?:타워|빌딩|몰|백화점)"
    );
    private static final Pattern BUILDING_CONTINUATION_PATTERN = Pattern.compile(
            "(?:타워|빌딩|백화점|쇼핑몰|몰)(?=\\s|\\)|$)"
    );
    private static final Pattern UNIT_CONTINUATION_PATTERN = Pattern.compile(
            "(?<![가-힣])(?:\\d+|[A-Za-z])\\s*(?:층|호|동|관)(?=\\s|[,)]|$)"
    );
    private static final Pattern LOT_CONTINUATION_PATTERN = Pattern.compile(
            "\\([^)]*[가-힣0-9-]+(?:동|읍|면|리)(?=[,)]|\\s)"
    );
    private static final Pattern SPLIT_NUMBER_PATTERN = Pattern.compile(
            "^\\d(?:\\.\\s*|\\s+)(?<detail>.+)$"
    );
    private static final Pattern BOUNDARY_PATTERN = Pattern.compile(
            "(?i).*(?:tel|전화|사업자|대표자?|거래일|결제일|승인일|판매일|영수증|영수번호"
                    + "|주문번호|매장명?|가맹점명|상호명?|점포명|지점명|상품명|품명"
                    + "|수량|단가|합계|결제금액|승인금액|공급가|부가세|과세|면세"
                    + "|할인|카드|현금).*"
    );
    private static final List<String> COMPOUND_WORDS = List.of(
            "백화점", "타워", "빌딩", "아파트", "오피스텔", "쇼핑몰"
    );
    private static final double MINIMUM_SCORE = 55.0;

    String extract(ReceiptOcrLayout layout) {
        List<Candidate> candidates = new ArrayList<>();
        // 라벨이 있는 주소는 일부 패턴이 깨져도 우선 후보로 평가한다.
        for (Line line : layout.lines()) {
            Matcher matcher = ADDRESS_LABEL_PATTERN.matcher(line.text());
            if (matcher.matches()) {
                collect(candidates, layout, line, sanitize(matcher.group("value")), true);
            }
        }
        // 라벨이 없는 주소는 주소 형식이 확인되는 경우에만 후보로 추가한다.
        for (Line line : layout.lines()) {
            String value = extractAddressSubstring(sanitize(line.text()));
            if (value != null) {
                collect(candidates, layout, line, value, false);
            }
        }
        return deduplicate(candidates).stream()
                .filter(candidate -> candidate.score() >= MINIMUM_SCORE)
                .max(candidateComparator())
                .map(Candidate::value)
                .orElse(null);
    }

    // 시작 행부터 주소 경계 문구가 나오기 전까지 연속된 상세주소만 단계별 후보로 만든다.
    private void collect(
            List<Candidate> candidates,
            ReceiptOcrLayout layout,
            Line startLine,
            String initialValue,
            boolean labeled
    ) {
        String accumulated = initialValue == null ? "" : initialValue;
        Line previousLine = startLine;
        addCandidate(candidates, accumulated, labeled, startLine, layout, 1);

        for (int offset = 1; offset <= 4; offset++) {
            int index = startLine.index() + offset;
            if (index >= layout.lines().size()) {
                break;
            }
            Line nextLine = layout.lines().get(index);
            String nextText = sanitize(nextLine.text());
            if (nextText.isBlank() || isBoundary(nextLine.text()) || isBoundary(nextText)) {
                break;
            }

            boolean continuation = hasCompleteEvidence(accumulated)
                    ? isDetailedContinuation(nextText)
                    : isAddressContinuation(nextText) || isStandaloneBuildingNumber(nextText);
            boolean adjacent = layout.isImmediatelyBelow(previousLine, nextLine);
            if (!continuation || (!labeled && !adjacent && offset > 1)) {
                break;
            }

            accumulated = join(accumulated, nextText, layout, previousLine, nextLine);
            addCandidate(candidates, accumulated, labeled, startLine, layout, offset + 1);
            previousLine = nextLine;
        }
    }

    private void addCandidate(
            List<Candidate> candidates,
            String rawValue,
            boolean labeled,
            Line line,
            ReceiptOcrLayout layout,
            int lineCount
    ) {
        String value = trimRepeatedRegionPrefix(sanitize(rawValue));
        if (value.isBlank()) {
            return;
        }

        boolean hasRegion = REGION_PATTERN.matcher(value).find();
        boolean hasDistrict = hasValidDistrict(value);
        boolean hasRoad = ROAD_PATTERN.matcher(value).find();
        boolean hasLot = LOT_PATTERN.matcher(value).find();
        boolean hasDamaged = !hasRoad && !hasLot && isDamagedAddress(value);
        boolean damagedContext = hasDamaged && hasDamagedContext(layout, line);
        if (!labeled && hasDamaged && !damagedContext) {
            return;
        }
        if ((!labeled && (!(hasRegion || hasDistrict) || (!hasRoad && !hasLot && !hasDamaged)))
                || (labeled && !hasRoad && !hasLot && !hasDamaged && !hasRegion)) {
            return;
        }

        double score = labeled ? 65.0 : 0.0;
        score += hasRegion ? 28.0 : 0.0;
        score += hasDistrict ? 12.0 : 0.0;
        score += hasRoad ? 38.0 : 0.0;
        score += hasLot ? 30.0 : 0.0;
        score += hasDamaged ? 28.0 : 0.0;
        score += damagedContext ? 8.0 : 0.0;
        score += hasDistrict && (hasRoad || hasLot || hasDamaged) ? 8.0 : 0.0;
        score += ADDRESS_UNIT_PATTERN.matcher(value).find() ? 7.0 : 0.0;
        score += Math.min(12.0, Math.max(0, lineCount - 1) * 4.0);
        score += line.confidence() * 4.0;
        candidates.add(new Candidate(value, score, line.index(), layout.verticalRatio(line)));
    }

    private String extractAddressSubstring(String value) {
        Matcher matcher = REGION_PATTERN.matcher(value);
        if (!matcher.find()) {
            matcher = findValidDistrict(value);
            if (matcher == null) {
                return null;
            }
        }
        return value.substring(matcher.start()).trim();
    }

    // 공백 제거는 알려진 복합어 또는 좌표로 분리 사실을 확인한 숫자에만 적용한다.
    private String join(
            String accumulated,
            String nextText,
            ReceiptOcrLayout layout,
            Line previousLine,
            Line nextLine
    ) {
        String separator = shouldJoinWithoutSpace(
                accumulated,
                nextText,
                layout,
                previousLine,
                nextLine
        ) ? "" : " ";
        return sanitize(accumulated + separator + nextText);
    }

    private boolean shouldJoinWithoutSpace(
            String accumulated,
            String nextText,
            ReceiptOcrLayout layout,
            Line previousLine,
            Line nextLine
    ) {
        if (formsSplitCompound(accumulated, nextText)) {
            return true;
        }
        if (!accumulated.matches(".*\\d$")) {
            return false;
        }
        Matcher matcher = SPLIT_NUMBER_PATTERN.matcher(nextText);
        if (!matcher.matches() || !isDetailedContinuation(matcher.group("detail"))) {
            return false;
        }
        if (layout.isLikelyWrappedContinuation(previousLine, nextLine)) {
            return true;
        }
        if (!layout.isSameRow(previousLine, nextLine)) {
            return false;
        }
        double height = Math.max(previousLine.bounds().height(), nextLine.bounds().height());
        double gap = layout.horizontalGap(previousLine, nextLine);
        return gap >= -height && gap <= height;
    }

    private boolean formsSplitCompound(String accumulated, String nextText) {
        String previousToken = accumulated.replaceFirst(".*\\s", "");
        String nextToken = nextText.replaceFirst("\\s.*", "");
        for (String compound : COMPOUND_WORDS) {
            for (int split = 1; split < compound.length(); split++) {
                if (previousToken.endsWith(compound.substring(0, split))
                        && nextToken.startsWith(compound.substring(split))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String sanitize(String address) {
        if (address == null) {
            return "";
        }
        return address
                .replaceFirst("(?i)\\s+(?:tel|전화)\\s*[:：]?.*$", "")
                .replaceFirst("^[\\s:：=,.-]+", "")
                .replaceAll("(?<=\\d)\\.\\s+(?=[A-Za-z가-힣])", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+,", ",")
                .trim();
    }

    private String trimRepeatedRegionPrefix(String value) {
        Matcher matcher = REGION_PATTERN.matcher(value);
        Map<String, Integer> starts = new HashMap<>();
        while (matcher.find()) {
            boolean boundaryStart = matcher.start() == 0
                    || Character.isWhitespace(value.charAt(matcher.start() - 1));
            boolean boundaryEnd = matcher.end() == value.length()
                    || Character.isWhitespace(value.charAt(matcher.end()));
            if (!boundaryStart || !boundaryEnd) {
                continue;
            }
            String key = compact(matcher.group()).replaceFirst(
                    "(?:특별자치도|특별자치시|특별시|광역시|도|시)$",
                    ""
            );
            Integer previous = starts.put(key, matcher.start());
            String remainder = value.substring(matcher.end()).trim();
            if (previous != null && (hasValidDistrict(remainder)
                    || ROAD_PATTERN.matcher(remainder).find()
                    || LOT_PATTERN.matcher(remainder).find())) {
                return value.substring(matcher.start()).trim();
            }
        }
        return value;
    }

    private boolean hasDamagedContext(ReceiptOcrLayout layout, Line candidate) {
        if (layout.verticalRatio(candidate) > 0.35) {
            return false;
        }
        int first = Math.max(0, candidate.index() - 2);
        int last = Math.min(layout.lines().size() - 1, candidate.index() + 2);
        for (int index = first; index <= last; index++) {
            if (index == candidate.index()) {
                continue;
            }
            String text = compact(layout.lines().get(index).text());
            if (containsAny(text, List.of("주소", "사업자", "대표", "전화", "tel"))
                    || layout.lines().get(index).text().matches(".*\\d{3}-\\d{2}-\\d{5}.*")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCompleteEvidence(String value) {
        return value != null && (ROAD_PATTERN.matcher(value).find()
                || LOT_PATTERN.matcher(value).find()
                || isDamagedAddress(value));
    }

    private boolean isAddressContinuation(String value) {
        return REGION_PATTERN.matcher(value).find()
                || startsWithDistrict(value)
                || ROAD_PATTERN.matcher(value).find()
                || LOT_PATTERN.matcher(value).find()
                || isDamagedAddress(value)
                || isDetailedContinuation(value)
                || value.matches("^[가-힣0-9·.-]{2,}(?:대로|로|길)$");
    }

    private boolean startsWithDistrict(String value) {
        Matcher matcher = findValidDistrict(value);
        return matcher != null && matcher.start() == 0;
    }

    private boolean isDetailedContinuation(String value) {
        if (isBoundary(value)) {
            return false;
        }
        return value.matches("^(?:층|호|동|관)$")
                || BUILDING_CONTINUATION_PATTERN.matcher(value).find()
                || UNIT_CONTINUATION_PATTERN.matcher(value).find()
                || LOT_CONTINUATION_PATTERN.matcher(value).find();
    }

    private boolean isStandaloneBuildingNumber(String value) {
        return value.matches("^[1-9]\\d{0,4}(?:-\\d+)?$");
    }

    private boolean isBoundary(String value) {
        return BOUNDARY_PATTERN.matcher(value).matches()
                || ReceiptDateTimeExtractor.findDateInLine(value) != null
                || ReceiptDateTimeExtractor.findTimeInLine(value) != null
                || value.matches("^[₩￦]?\\s*\\d{1,3}(?:[,．.]\\d{3})+\\s*원?$");
    }

    private boolean isDamagedAddress(String value) {
        return hasValidDistrict(value) && DAMAGED_PATTERN.matcher(value).find();
    }

    private boolean hasValidDistrict(String value) {
        return findValidDistrict(value) != null;
    }

    private Matcher findValidDistrict(String value) {
        Matcher matcher = DISTRICT_PATTERN.matcher(value);
        while (matcher.find()) {
            if (!NON_ADDRESS_DISTRICT_PATTERN.matcher(compact(matcher.group())).matches()) {
                return matcher;
            }
        }
        return null;
    }

    private List<Candidate> deduplicate(List<Candidate> candidates) {
        Map<String, Candidate> unique = new HashMap<>();
        Comparator<Candidate> comparator = candidateComparator();
        for (Candidate candidate : candidates) {
            unique.merge(compact(candidate.value()), candidate, (first, second) ->
                    comparator.compare(first, second) >= 0 ? first : second
            );
        }
        return List.copyOf(unique.values());
    }

    private Comparator<Candidate> candidateComparator() {
        return Comparator.comparingDouble(Candidate::score)
                .thenComparing(Comparator.comparingDouble(Candidate::verticalRatio).reversed())
                .thenComparing(Comparator.comparingInt(Candidate::lineIndex).reversed());
    }

    private boolean containsAny(String value, List<String> parts) {
        return parts.stream().map(ReceiptAddressExtractor::compact).anyMatch(value::contains);
    }

    private static String compact(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private record Candidate(String value, double score, int lineIndex, double verticalRatio) {
    }
}
