package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.service.ReceiptOcrLayout.Line;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 영수증에 나온 날짜와 시간 중 실제 결제 시점으로 보이는 값을 고른다.
 *
 * 점, 슬래시, 하이픈으로 구분된 날짜와 한글 날짜, yyyyMMdd 형식을 찾는다.
 * 시간은 오전과 오후가 붙은 형식도 처리한다.
 * 날짜와 시간이 같은 행에 함께 있거나 해당 행에 거래, 결제, 승인 같은 문구가 있으면 우선하고,
 * 반품기한이나 유효기간, 주문번호와 승인번호에 붙은 값은 우선순위를 낮춘다.
 *
 * 날짜와 시간은 따로 고르기 때문에 둘 중 하나만 읽혀도 찾은 값은 반환한다.
 * 실제로 존재하지 않는 날짜와 시간은 후보에서 제외한다.
 */
final class ReceiptDateTimeExtractor {

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{2}|(?:19|20)\\d{2})\\s*([./-])\\s*(\\d{1,2})"
                    + "\\s*\\2\\s*(\\d{1,2})(?!\\d)"
    );
    private static final Pattern KOREAN_DATE_PATTERN = Pattern.compile(
            "(?<!\\d)((?:19|20)\\d{2})\\s*년\\s*(\\d{1,2})\\s*월"
                    + "\\s*(\\d{1,2})\\s*일"
    );
    private static final Pattern COMPACT_DATE_PATTERN = Pattern.compile(
            "(?<!\\d)((?:19|20)\\d{2})(\\d{2})(\\d{2})(?!\\d)"
    );
    private static final String MERIDIEM_PATTERN =
            "(?:(?<![가-힣])(?:오전|오후)(?![가-힣])|(?<![A-Za-z])(?:AM|PM)(?![A-Za-z]))";
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:(?<prefix>" + MERIDIEM_PATTERN + ")\\s*)?"
                    + "(?<!\\d)(?<hour>[01]?\\d|2[0-3])\\s*:\\s*(?<minute>[0-5]\\d)"
                    + "(?:\\s*:\\s*(?<second>[0-5]\\d))?(?!\\d)"
                    + "(?:\\s*(?<suffix>" + MERIDIEM_PATTERN + "))?",
            Pattern.CASE_INSENSITIVE
    );
    private static final List<String> POSITIVE_LABELS = List.of(
            "거래일시", "결제일시", "승인일시", "판매일시", "발행일시", "주문접수시간",
            "거래일", "결제일", "승인일", "판매일", "판매시간", "매출일", "계산일자", "구매"
    );
    private static final List<String> NEGATIVE_LABELS = List.of(
            "반품기한", "유효기간", "쿠폰기간", "원거래일", "영수번호", "주문번호",
            "승인번호", "카드번호", "사업자번호", "가맹점번호", "회원번호", "일련번호",
            "처리번호", "거래번호", "no", "pos", "bill"
    );

    Result extract(List<Line> lines) {
        return new Result(findDate(lines), findTime(lines));
    }

    static LocalDate findDateInLine(String line) {
        LocalDate date = createSeparatedDate(DATE_PATTERN.matcher(line));
        if (date != null) {
            return date;
        }
        date = createDate(KOREAN_DATE_PATTERN.matcher(line), 1, 2, 3);
        return date != null
                ? date
                : createDate(COMPACT_DATE_PATTERN.matcher(line), 1, 2, 3);
    }

    static LocalTime findTimeInLine(String line) {
        Matcher matcher = TIME_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        try {
            String prefix = matcher.group("prefix");
            String suffix = matcher.group("suffix");
            if (prefix != null && suffix != null && isAfternoon(prefix) != isAfternoon(suffix)) {
                return null;
            }

            int hour = Integer.parseInt(matcher.group("hour"));
            String meridiem = prefix != null ? prefix : suffix;
            if (meridiem != null) {
                if (hour < 1 || hour > 12) {
                    return null;
                }
                if (isAfternoon(meridiem)) {
                    hour = hour == 12 ? 12 : hour + 12;
                } else if (hour == 12) {
                    hour = 0;
                }
            }

            int second = matcher.group("second") == null
                    ? 0
                    : Integer.parseInt(matcher.group("second"));
            return LocalTime.of(hour, Integer.parseInt(matcher.group("minute")), second);
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private LocalDate findDate(List<Line> lines) {
        return lines.stream()
                .map(line -> candidate(findDateInLine(line.text()), line, findTimeInLine(line.text())))
                .filter(candidate -> candidate.value() != null)
                .filter(candidate -> candidate.score() >= 0.0)
                .max(candidateComparator())
                .map(Candidate::value)
                .orElse(null);
    }

    private LocalTime findTime(List<Line> lines) {
        return lines.stream()
                .map(line -> candidate(findTimeInLine(line.text()), line, findDateInLine(line.text())))
                .filter(candidate -> candidate.value() != null)
                .filter(candidate -> candidate.score() >= 0.0)
                .max(candidateComparator())
                .map(Candidate::value)
                .orElse(null);
    }

    // 점수는 정답 확률이 아니라 같은 문서 안 후보들의 상대적인 우선순위다.
    private <T> Candidate<T> candidate(T value, Line line, Object pairedValue) {
        String compactText = compact(line.text());
        double score = 20.0 + line.confidence() * 5.0;
        if (containsAny(compactText, POSITIVE_LABELS)) {
            score += 100.0;
        }
        if (containsAny(compactText, NEGATIVE_LABELS)) {
            score -= 90.0;
        }
        if (pairedValue != null) {
            score += 18.0;
        }
        return new Candidate<>(value, score, line.index(), line.confidence());
    }

    private <T> Comparator<Candidate<T>> candidateComparator() {
        return Comparator.<Candidate<T>>comparingDouble(Candidate::score)
                .thenComparingDouble(Candidate::confidence)
                .thenComparing(Comparator.comparingInt(Candidate<T>::lineIndex).reversed());
    }

    private static LocalDate createSeparatedDate(Matcher matcher) {
        return matcher.find()
                ? createDate(matcher.group(1), matcher.group(3), matcher.group(4))
                : null;
    }

    private static LocalDate createDate(
            Matcher matcher,
            int yearGroup,
            int monthGroup,
            int dayGroup
    ) {
        return matcher.find()
                ? createDate(
                matcher.group(yearGroup),
                matcher.group(monthGroup),
                matcher.group(dayGroup)
        )
                : null;
    }

    private static LocalDate createDate(String yearValue, String monthValue, String dayValue) {
        try {
            int year = Integer.parseInt(yearValue);
            return LocalDate.of(
                    year < 100 ? year + 2000 : year,
                    Integer.parseInt(monthValue),
                    Integer.parseInt(dayValue)
            );
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isAfternoon(String meridiem) {
        return "오후".equals(meridiem) || "PM".equalsIgnoreCase(meridiem);
    }

    private static boolean containsAny(String compactText, List<String> values) {
        return values.stream().map(ReceiptDateTimeExtractor::compact).anyMatch(compactText::contains);
    }

    private static String compact(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    record Result(LocalDate date, LocalTime time) {
    }

    private record Candidate<T>(T value, double score, int lineIndex, double confidence) {
    }
}
