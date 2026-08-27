package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.ReceiptOcrDocument;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.Point;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.TextField;
import kr.chapchap.consumption.exception.ConsumptionErrorCode;
import kr.chapchap.consumption.infra.config.ClovaOcrProperties;
import kr.chapchap.consumption.infra.config.ReceiptOcrInfraConfig;
import kr.chapchap.consumption.infra.external.ocr.ClovaOcrClient;
import kr.chapchap.consumption.infra.external.ocr.ClovaOcrRateLimiter;
import kr.chapchap.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

@Slf4j
@EnabledIfEnvironmentVariable(
        named = "RUN_CLOVA_RECEIPT_OCR_LIVE_TEST",
        matches = "true"
)
class ReceiptOcrSamplesLiveTest {

    private static final String SAMPLE_DIRECTORY = "docs/receipt-ocr-samples-20260826";
    private static final String SAMPLE_LIMIT_ENVIRONMENT_VARIABLE =
            "CLOVA_RECEIPT_OCR_SAMPLE_LIMIT";
    private static final String SAMPLE_FILES_ENVIRONMENT_VARIABLE =
            "CLOVA_RECEIPT_OCR_SAMPLE_FILES";
    private static final int EXPECTED_SAMPLE_COUNT = 30;
    private static final Duration REQUEST_INTERVAL = Duration.ofMillis(1_100);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration MAX_WAIT = Duration.ofSeconds(5);

    @Test
    void 실제_CLOVA_OCR로_영수증_샘플_30개의_후처리_전후_결과를_확인한다() throws IOException {
        // given
        ReceiptImageValidator imageValidator = new ReceiptImageValidator();
        ReceiptOcrParser receiptOcrParser = new ReceiptOcrParser();
        ClovaOcrClient clovaOcrClient = createClovaOcrClient();
        List<Path> allSamplePaths = findSamplePaths();
        List<Path> samplePaths = selectSamplePaths(allSamplePaths);
        Path reportPath = createReportPath(samplePaths.size());
        StringBuilder report = new StringBuilder(
                "# 실제 CLOVA 영수증 OCR 구조화 후처리 결과\n\n"
                        + "> 검출 수는 non-null 개수이며 정확도를 뜻하지 않습니다.\n\n"
        );
        assertThat(allSamplePaths).hasSize(EXPECTED_SAMPLE_COUNT);
        assertThat(samplePaths).isNotEmpty();

        int recognitionFailureCount = 0;
        int emptyLinesCount = 0;
        int storeNameCount = 0;
        int addressCount = 0;
        int purchaseDateCount = 0;
        int purchaseTimeCount = 0;
        int amountCount = 0;

        // when
        for (Path samplePath : samplePaths) {
            byte[] content = Files.readAllBytes(samplePath);
            String contentType = imageValidator.validateAndGetContentType(content);

            ReceiptOcrDocument document;
            try {
                document = clovaOcrClient.recognize(content, contentType);
            } catch (BusinessException exception) {
                if (exception.getErrorCode()
                        != ConsumptionErrorCode.RECEIPT_OCR_RECOGNITION_FAILED) {
                    throw exception;
                }
                recognitionFailureCount++;
                appendRecognitionFailure(report, samplePath);
                writeReport(reportPath, report);
                log.info(
                        "LIVE_RECEIPT_OCR_RESULT file={}, result=RECOGNITION_FAILED",
                        samplePath.getFileName()
                );
                continue;
            }

            List<String> lines = document.lines();
            ReceiptOcrParser.ParsedReceipt parsedReceipt = receiptOcrParser.parse(document);
            if (lines.isEmpty()) {
                emptyLinesCount++;
            }
            storeNameCount += parsedReceipt.storeName() != null ? 1 : 0;
            addressCount += parsedReceipt.address() != null ? 1 : 0;
            purchaseDateCount += parsedReceipt.purchaseDate() != null ? 1 : 0;
            purchaseTimeCount += parsedReceipt.purchaseTime() != null ? 1 : 0;
            amountCount += parsedReceipt.amount() != null ? 1 : 0;

            appendResult(report, samplePath, document, lines, parsedReceipt);
            writeReport(reportPath, report);
            log.info(
                    "LIVE_RECEIPT_OCR_RESULT file={}, lineCount={}, result=SUCCESS",
                    samplePath.getFileName(),
                    lines.size()
            );
        }

        // then
        appendSummary(
                report,
                samplePaths.size(),
                recognitionFailureCount,
                emptyLinesCount,
                storeNameCount,
                addressCount,
                purchaseDateCount,
                purchaseTimeCount,
                amountCount
        );
        writeReport(reportPath, report);
        log.info(
                "LIVE_RECEIPT_OCR_SUMMARY attempts={}, recognitionFailures={}, emptyLines={}, "
                        + "storeNames={}, addresses={}, purchaseDates={}, purchaseTimes={}, amounts={}, "
                        + "reportPath={}",
                samplePaths.size(),
                recognitionFailureCount,
                emptyLinesCount,
                storeNameCount,
                addressCount,
                purchaseDateCount,
                purchaseTimeCount,
                amountCount,
                reportPath
        );
        assertThat(recognitionFailureCount).isZero();
        assertThat(emptyLinesCount).isZero();
    }

    private ClovaOcrClient createClovaOcrClient() {
        ClovaOcrProperties properties = new ClovaOcrProperties(
                URI.create(requireEnvironmentVariable("CLOVA_OCR_INVOKE_URL")),
                requireEnvironmentVariable("CLOVA_OCR_SECRET_KEY"),
                requireEnvironmentVariable("CLOVA_OCR_API_GATEWAY_KEY"),
                CONNECT_TIMEOUT,
                READ_TIMEOUT,
                REQUEST_INTERVAL,
                MAX_WAIT
        );
        RestClient restClient = new ReceiptOcrInfraConfig().clovaOcrRestClient(
                RestClient.builder(),
                properties
        );
        return new ClovaOcrClient(
                restClient,
                properties,
                createRateLimiter(),
                Clock.systemUTC()
        );
    }

    private ClovaOcrRateLimiter createRateLimiter() {
        ClovaOcrRateLimiter rateLimiter = mock(ClovaOcrRateLimiter.class);
        AtomicLong nextAllowedAt = new AtomicLong();
        willAnswer(invocation -> {
            long waitMillis = nextAllowedAt.get() - System.currentTimeMillis();
            if (waitMillis > 0) {
                Thread.sleep(waitMillis);
            }
            nextAllowedAt.set(System.currentTimeMillis() + REQUEST_INTERVAL.toMillis());
            return null;
        }).given(rateLimiter).awaitPermit();
        return rateLimiter;
    }

    private List<Path> findSamplePaths() throws IOException {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path sampleDirectory = List.of(
                        workingDirectory.resolve(SAMPLE_DIRECTORY),
                        workingDirectory.resolve("..").resolve(SAMPLE_DIRECTORY)
                ).stream()
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "영수증 OCR 샘플 디렉터리를 찾을 수 없습니다."
                ));

        try (var paths = Files.list(sampleDirectory)) {
            return paths
                    .filter(path -> path.getFileName().toString().matches("receipt_\\d{2}\\.jpeg"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private Path createReportPath(int selectedSampleCount) throws IOException {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path moduleDirectory = Files.isDirectory(workingDirectory.resolve("src/test"))
                ? workingDirectory
                : workingDirectory.resolve("module-consumption");
        Path reportDirectory = moduleDirectory.resolve("build/reports/receipt-ocr-live");
        Files.createDirectories(reportDirectory);
        String reportName = selectedSampleCount == EXPECTED_SAMPLE_COUNT
                ? "report-after.md"
                : "report-selected.md";
        return reportDirectory.resolve(reportName);
    }

    private void appendRecognitionFailure(StringBuilder report, Path samplePath) {
        report.append("## ")
                .append(samplePath.getFileName())
                .append("\n\n- 결과: `RECOGNITION_FAILED`\n\n");
    }

    private void appendResult(
            StringBuilder report,
            Path samplePath,
            ReceiptOcrDocument document,
            List<String> lines,
            ReceiptOcrParser.ParsedReceipt parsedReceipt
    ) {
        report.append("## ")
                .append(samplePath.getFileName())
                .append("\n\n### 후처리 전: CLOVA OCR 원본 필드\n\n```text\n");
        for (int index = 0; index < document.fields().size(); index++) {
            TextField field = document.fields().get(index);
            String confidence = field.confidencePresent()
                    ? String.format("%.6f", field.confidence())
                    : "missing";
            report.append(String.format(
                    "%03d | confidence=%s | lineBreak=%s | bounds=%s | %s%n",
                    index + 1,
                    confidence,
                    field.lineBreak(),
                    formatBounds(field.boundingVertices()),
                    field.text().replace("```", "'''")
            ));
        }
        report.append("```\n\n### ReceiptOcrDocument에서 재구성한 참고 행\n\n```text\n");
        for (int index = 0; index < lines.size(); index++) {
            report.append(String.format(
                    "%03d | %s%n",
                    index + 1,
                    lines.get(index).replace("```", "'''"))
            );
        }
        report.append("```\n\n### 후처리 후: ReceiptOcrParser 결과\n\n```text\n")
                .append("storeName=").append(parsedReceipt.storeName()).append('\n')
                .append("address=").append(parsedReceipt.address()).append('\n')
                .append("purchaseDate=").append(parsedReceipt.purchaseDate()).append('\n')
                .append("purchaseTime=").append(parsedReceipt.purchaseTime()).append('\n')
                .append("amount=").append(parsedReceipt.amount()).append('\n')
                .append("```\n\n");
    }

    private String formatBounds(List<Point> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return "none";
        }

        double left = vertices.stream().mapToDouble(Point::x).min().orElse(Double.NaN);
        double top = vertices.stream().mapToDouble(Point::y).min().orElse(Double.NaN);
        double right = vertices.stream().mapToDouble(Point::x).max().orElse(Double.NaN);
        double bottom = vertices.stream().mapToDouble(Point::y).max().orElse(Double.NaN);
        return String.format("[%.1f,%.1f]-[%.1f,%.1f]", left, top, right, bottom);
    }

    private void appendSummary(
            StringBuilder report,
            int attempts,
            int recognitionFailures,
            int emptyLines,
            int storeNames,
            int addresses,
            int purchaseDates,
            int purchaseTimes,
            int amounts
    ) {
        report.append("## 전체 요약\n\n")
                .append("- 시도: ").append(attempts).append('\n')
                .append("- 인식 실패: ").append(recognitionFailures).append('\n')
                .append("- OCR 라인 없음: ").append(emptyLines).append('\n')
                .append("- 상호명 non-null: ").append(storeNames).append('\n')
                .append("- 주소 non-null: ").append(addresses).append('\n')
                .append("- 구매일 non-null: ").append(purchaseDates).append('\n')
                .append("- 구매시간 non-null: ").append(purchaseTimes).append('\n')
                .append("- 금액 non-null: ").append(amounts).append('\n');
    }

    private void writeReport(Path reportPath, StringBuilder report) throws IOException {
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
    }

    private String requireEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 환경변수가 필요합니다.");
        }
        return value.trim();
    }

    private int resolveSampleLimit() {
        String value = System.getenv(SAMPLE_LIMIT_ENVIRONMENT_VARIABLE);
        if (value == null || value.isBlank()) {
            return EXPECTED_SAMPLE_COUNT;
        }

        try {
            int sampleLimit = Integer.parseInt(value.trim());
            if (sampleLimit < 1 || sampleLimit > EXPECTED_SAMPLE_COUNT) {
                throw new IllegalArgumentException(
                        SAMPLE_LIMIT_ENVIRONMENT_VARIABLE + "는 1 이상 30 이하여야 합니다."
                );
            }
            return sampleLimit;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    SAMPLE_LIMIT_ENVIRONMENT_VARIABLE + "는 정수여야 합니다.",
                    exception
            );
        }
    }

    private List<Path> selectSamplePaths(List<Path> allSamplePaths) {
        String selectedFiles = System.getenv(SAMPLE_FILES_ENVIRONMENT_VARIABLE);
        if (selectedFiles == null || selectedFiles.isBlank()) {
            return allSamplePaths.stream()
                    .limit(resolveSampleLimit())
                    .toList();
        }

        Set<String> requestedFileNames = new LinkedHashSet<>(List.of(
                selectedFiles.trim().split("\\s*,\\s*")
        ));
        List<Path> selectedSamplePaths = allSamplePaths.stream()
                .filter(path -> requestedFileNames.contains(path.getFileName().toString()))
                .toList();
        if (selectedSamplePaths.size() != requestedFileNames.size()) {
            throw new IllegalArgumentException(
                    SAMPLE_FILES_ENVIRONMENT_VARIABLE + "에 존재하지 않는 샘플 파일이 있습니다."
            );
        }
        return selectedSamplePaths;
    }
}
