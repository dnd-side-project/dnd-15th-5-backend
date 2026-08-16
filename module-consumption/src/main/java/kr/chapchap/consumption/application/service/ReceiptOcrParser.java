package kr.chapchap.consumption.application.service;

import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReceiptOcrParser {

    private static final Pattern STORE_LABEL_PATTERN = Pattern.compile(
            "^(?:상호(?:명)?|매장명|가맹점명)\\s*[:：]?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ADDRESS_LABEL_PATTERN = Pattern.compile(
            "^(?:주소|사업장\\s*소재지|소재지)\\s*[:：]?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ROAD_ADDRESS_PATTERN = Pattern.compile(
            ".*[가-힣0-9]+(?:대로|로|길)\\s*\\d+(?:-\\d+)?(?:\\s.*)?$"
    );
    private static final Pattern LOT_NUMBER_ADDRESS_PATTERN = Pattern.compile(
            ".*(?:시|도|구|군)\\s+.*(?:동|읍|면|리)\\s*\\d+(?:-\\d+)?(?:\\s.*)?$"
    );
    private static final Pattern REGION_ADDRESS_FRAGMENT_PATTERN = Pattern.compile(
            ".*(?:[가-힣]+특별시|[가-힣]+광역시|[가-힣]+특별자치시|[가-힣]+특별자치도"
                    + "|[가-힣]+도|[가-힣]+시|[가-힣]+군|[가-힣]+구|[가-힣]+동"
                    + "|[가-힣]+읍|[가-힣]+면)(?:\\s|$).*"
    );
    private static final Pattern ROAD_ADDRESS_FRAGMENT_PATTERN = Pattern.compile(
            ".*[가-힣0-9]+(?:대로|로|길)(?:\\s|$).*"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{2}|(?:19|20)\\d{2})\\s*[./-]\\s*(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})(?!\\d)"
    );
    private static final Pattern KOREAN_DATE_PATTERN = Pattern.compile(
            "(?<!\\d)((?:19|20)\\d{2})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일"
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
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d+(?:\\s*,\\s*\\d{3})*)(?!\\d)"
    );
    private static final Pattern CURRENCY_AMOUNT_PATTERN = Pattern.compile(
            "(?<!\\d)(?:[₩￦]\\s*(\\d+(?:\\s*,\\s*\\d{3})*)"
                    + "|(\\d+(?:\\s*,\\s*\\d{3})*)\\s*원)(?!\\d)"
    );
    private static final Pattern STANDALONE_AMOUNT_PATTERN = Pattern.compile(
            "^\\s*[₩￦]?\\s*\\d+(?:\\s*,\\s*\\d{3})*\\s*원?\\s*$"
    );
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[가-힣A-Za-z].*");

    private static final List<String> DATE_TIME_LABELS = List.of(
            "거래일시", "결제일시", "승인일시", "판매일시", "거래일", "결제일", "승인일"
    );
    private static final List<String> AMOUNT_LABELS = List.of(
            "총 결제금액", "결제금액", "승인금액", "청구금액", "카드결제", "현금결제",
            "매출금액", "받을금액", "총액", "합계"
    );
    private static final Set<String> STORE_EXCLUDED_WORDS = Set.of(
            "영수증", "신용카드", "매출전표", "카드전표", "정상승인", "고객용", "사업자",
            "대표자", "전화", "주문번호", "거래일시", "승인일시", "결제일시", "합계", "총액"
    );
    private static final List<String> AMOUNT_EXCLUDED_WORDS = List.of(
            "부가세", "vat", "공급가", "과세", "면세", "할인", "거스름", "잔액", "포인트",
            "쿠폰", "수량", "단가", "받은금액", "현금수령", "예수금"
    );

    ParsedReceipt parse(List<String> rawLines) {
        List<String> lines = normalizeLines(rawLines);
        LocalDate purchaseDate = findDate(lines);
        LocalTime purchaseTime = findTime(lines);
        return new ParsedReceipt(
                findStoreName(lines),
                findAddress(lines),
                purchaseDate,
                purchaseTime,
                findAmount(lines)
        );
    }

    private List<String> normalizeLines(List<String> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return List.of();
        }

        return rawLines.stream()
                .filter(line -> line != null && !line.isBlank())
                .map(line -> line.trim().replaceAll("\\s+", " "))
                .toList();
    }

    private String findStoreName(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            Matcher matcher = STORE_LABEL_PATTERN.matcher(lines.get(index));
            if (!matcher.matches()) {
                continue;
            }

            String value = matcher.group(1).trim();
            if (!value.isBlank()) {
                return value;
            }
            if (index + 1 < lines.size() && isStoreNameCandidate(lines.get(index + 1))) {
                return lines.get(index + 1);
            }
        }

        return lines.stream()
                .limit(8)
                .filter(this::isStoreNameCandidate)
                .findFirst()
                .orElse(null);
    }

    private boolean isStoreNameCandidate(String line) {
        String lowerCaseLine = line.toLowerCase(Locale.ROOT);
        String compactLine = lowerCaseLine.replaceAll("\\s+", "");
        if (line.length() < 2 || line.length() > 50 || !LETTER_PATTERN.matcher(line).matches()) {
            return false;
        }
        if (STORE_EXCLUDED_WORDS.stream().anyMatch(compactLine::contains)) {
            return false;
        }
        return !looksLikeAddress(line)
                && findDateInLine(line) == null
                && findTimeInLine(line) == null
                && !lowerCaseLine.matches(".*(?:tel|전화)\\s*[:：]?.*\\d.*");
    }

    private String findAddress(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            Matcher matcher = ADDRESS_LABEL_PATTERN.matcher(lines.get(index));
            if (!matcher.matches()) {
                continue;
            }

            String value = sanitizeAddress(matcher.group(1));
            String accumulated = value;
            for (int offset = 1; offset <= 3 && index + offset < lines.size(); offset++) {
                if (looksLikeAddress(accumulated)) {
                    return accumulated;
                }
                accumulated = sanitizeAddress(
                        accumulated + " " + lines.get(index + offset)
                );
            }
            if (looksLikeAddress(accumulated)) {
                return accumulated;
            }
            if (!value.isBlank()) {
                return value;
            }
        }

        for (int index = 0; index < lines.size(); index++) {
            String accumulated = "";
            for (int offset = 0; offset <= 2 && index + offset < lines.size(); offset++) {
                accumulated = sanitizeAddress(
                        accumulated + " " + lines.get(index + offset)
                );
                if (looksLikeAddress(accumulated)) {
                    return accumulated;
                }
                if (offset == 0 && !looksLikeAddressFragment(accumulated)) {
                    break;
                }
            }
        }
        return null;
    }

    private boolean looksLikeAddress(String line) {
        return ROAD_ADDRESS_PATTERN.matcher(line).matches()
                || LOT_NUMBER_ADDRESS_PATTERN.matcher(line).matches();
    }

    private boolean looksLikeAddressFragment(String line) {
        return REGION_ADDRESS_FRAGMENT_PATTERN.matcher(line).matches()
                || ROAD_ADDRESS_FRAGMENT_PATTERN.matcher(line).matches();
    }

    private String sanitizeAddress(String address) {
        return address
                .replaceFirst("(?i)\\s+(?:tel|전화)\\s*[:：]?.*$", "")
                .trim();
    }

    private LocalDate findDate(List<String> lines) {
        for (String label : DATE_TIME_LABELS) {
            for (String line : lines) {
                if (line.contains(label)) {
                    LocalDate date = findDateInLine(line);
                    if (date != null) {
                        return date;
                    }
                }
            }
        }

        return lines.stream()
                .map(this::findDateInLine)
                .filter(date -> date != null)
                .findFirst()
                .orElse(null);
    }

    private LocalDate findDateInLine(String line) {
        LocalDate date = createDate(DATE_PATTERN.matcher(line));
        return date != null ? date : createDate(KOREAN_DATE_PATTERN.matcher(line));
    }

    private LocalDate createDate(Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }

        try {
            int year = Integer.parseInt(matcher.group(1));
            if (year < 100) {
                year += 2000;
            }
            return LocalDate.of(
                    year,
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            );
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private LocalTime findTime(List<String> lines) {
        for (String label : DATE_TIME_LABELS) {
            for (String line : lines) {
                if (line.contains(label)) {
                    LocalTime time = findTimeInLine(line);
                    if (time != null) {
                        return time;
                    }
                }
            }
        }

        return lines.stream()
                .map(this::findTimeInLine)
                .filter(time -> time != null)
                .findFirst()
                .orElse(null);
    }

    private LocalTime findTimeInLine(String line) {
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

            int second = matcher.group("second") == null ? 0 : Integer.parseInt(matcher.group("second"));
            return LocalTime.of(hour, Integer.parseInt(matcher.group("minute")), second);
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private boolean isAfternoon(String meridiem) {
        return "오후".equals(meridiem) || "PM".equalsIgnoreCase(meridiem);
    }

    private Long findAmount(List<String> lines) {
        for (String label : AMOUNT_LABELS) {
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                String compactLine = line.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
                String compactLabel = label.toLowerCase(Locale.ROOT).replace(" ", "");
                int labelIndex = compactLine.indexOf(compactLabel);
                if (labelIndex < 0 || isExcludedLabel(compactLine, labelIndex)) {
                    continue;
                }

                String suffix = compactLine.substring(labelIndex + compactLabel.length());
                Long amount = findFirstAmount(suffix);
                if (amount != null) {
                    return amount;
                }
                if (index + 1 < lines.size()
                        && STANDALONE_AMOUNT_PATTERN.matcher(lines.get(index + 1)).matches()) {
                    amount = findFirstAmount(lines.get(index + 1));
                    if (amount != null) {
                        return amount;
                    }
                }
            }
        }

        List<Long> candidates = new ArrayList<>();
        for (String line : lines) {
            String lowerCaseLine = line.toLowerCase(Locale.ROOT);
            if (AMOUNT_EXCLUDED_WORDS.stream().anyMatch(lowerCaseLine::contains)) {
                continue;
            }
            if (!line.contains("원") && !line.contains("₩") && !line.contains("￦")) {
                continue;
            }
            Long amount = findCurrencyAmount(line);
            if (amount != null) {
                candidates.add(amount);
            }
        }
        return candidates.isEmpty() ? null : candidates.getLast();
    }

    private boolean isExcludedLabel(String compactLine, int labelIndex) {
        String prefix = compactLine.substring(0, labelIndex);
        return AMOUNT_EXCLUDED_WORDS.stream()
                .map(word -> word.replaceAll("\\s+", ""))
                .anyMatch(prefix::endsWith);
    }

    private Long findCurrencyAmount(String line) {
        Matcher matcher = CURRENCY_AMOUNT_PATTERN.matcher(line);
        Long amount = null;
        while (matcher.find()) {
            Long candidate = parseAmount(getCurrencyAmountValue(matcher));
            if (candidate != null) {
                amount = candidate;
            }
        }
        return amount;
    }

    private Long findFirstAmount(String line) {
        Matcher currencyMatcher = CURRENCY_AMOUNT_PATTERN.matcher(line);
        if (currencyMatcher.find()) {
            return parseAmount(getCurrencyAmountValue(currencyMatcher));
        }
        Matcher matcher = AMOUNT_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        return parseAmount(matcher.group(1));
    }

    private String getCurrencyAmountValue(Matcher matcher) {
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private Long parseAmount(String value) {
        try {
            long amount = Long.parseLong(value.replaceAll("[,\\s]", ""));
            return amount > 0 ? amount : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    record ParsedReceipt(
            String storeName,
            String address,
            LocalDate purchaseDate,
            LocalTime purchaseTime,
            Long amount
    ) {
    }
}
