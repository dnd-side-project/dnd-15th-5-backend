package kr.chapchap.place;

import kr.chapchap.core.exception.BusinessException;
import kr.chapchap.core.test.TestcontainersConfiguration;
import kr.chapchap.place.application.info.GooglePlaceTextSearchInfo;
import kr.chapchap.place.application.port.GooglePlaceTextSearchPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "RUN_RECEIPT_GOOGLE_PLACE_SEARCH_LIVE_TEST",
        matches = "true"
)
class ReceiptGooglePlaceSearchLiveTest {

    private static final long REQUEST_INTERVAL_MS = 2_100;
    private static final int EXPECTED_CASE_COUNT = 20;

    private final GooglePlaceTextSearchPort googlePlaceTextSearchPort;

    @Autowired
    ReceiptGooglePlaceSearchLiveTest(GooglePlaceTextSearchPort googlePlaceTextSearchPort) {
        this.googlePlaceTextSearchPort = googlePlaceTextSearchPort;
    }

    @Test
    void 현재_OCR_파서의_단일_영수증_20건으로_Google_장소를_검색한다()
            throws IOException, InterruptedException {
        // given
        List<SearchCase> cases = searchCases();
        Path reportPath = findSampleDirectory().resolve("google-place-search-after.md");
        StringBuilder report = new StringBuilder(
                "# 현재 OCR 파서 기반 Google Places Text Search 결과\n\n"
                        + "> 대상은 단일 영수증 평가군 `receipt_01`~`receipt_20`이다. "
                        + "Text Search만 호출하며 썸네일은 조회하지 않는다.\n\n"
                        + "| 영수증 | 입력 상호명 | 입력 주소 | Google 장소명 | Google 주소 | 결과 |\n"
                        + "| --- | --- | --- | --- | --- | --- |\n"
        );
        int requestCount = 0;
        int resultCount = 0;
        int emptyCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        assertThat(cases).hasSize(EXPECTED_CASE_COUNT);

        // when
        for (int index = 0; index < cases.size(); index++) {
            SearchCase searchCase = cases.get(index);
            if (searchCase.storeName() == null) {
                skippedCount++;
                appendRow(report, searchCase, null, "SKIPPED_NO_STORE_NAME");
                writeReport(reportPath, report);
                continue;
            }

            String textQuery = searchCase.address() == null
                    ? searchCase.storeName()
                    : searchCase.storeName() + " " + searchCase.address();
            requestCount++;
            try {
                Optional<GooglePlaceTextSearchInfo> result =
                        googlePlaceTextSearchPort.searchFirst(textQuery);
                if (result.isPresent()) {
                    resultCount++;
                    appendRow(report, searchCase, result.get(), "FOUND");
                } else {
                    emptyCount++;
                    appendRow(report, searchCase, null, "EMPTY");
                }
            } catch (BusinessException exception) {
                errorCount++;
                appendRow(
                        report,
                        searchCase,
                        null,
                        "ERROR(" + exception.getErrorCode().getCode() + ")"
                );
                writeReport(reportPath, report);
                throw exception;
            }
            writeReport(reportPath, report);

            if (index < cases.size() - 1) {
                Thread.sleep(REQUEST_INTERVAL_MS);
            }
        }

        // then
        report.append("\n## 실행 요약\n\n")
                .append("- 평가 대상: ").append(cases.size()).append('\n')
                .append("- Text Search 호출: ").append(requestCount).append('\n')
                .append("- 결과 반환: ").append(resultCount).append('\n')
                .append("- 결과 없음: ").append(emptyCount).append('\n')
                .append("- 상호명 부재로 생략: ").append(skippedCount).append('\n')
                .append("- 오류: ").append(errorCount).append('\n');
        writeReport(reportPath, report);

        assertThat(requestCount + skippedCount).isEqualTo(cases.size());
        assertThat(resultCount + emptyCount + errorCount).isEqualTo(requestCount);
        assertThat(errorCount).isZero();
    }

    private void appendRow(
            StringBuilder report,
            SearchCase searchCase,
            GooglePlaceTextSearchInfo place,
            String result
    ) {
        report.append("| ")
                .append(String.format("%02d", searchCase.receiptNumber()))
                .append(" | ").append(markdownValue(searchCase.storeName()))
                .append(" | ").append(markdownValue(searchCase.address()))
                .append(" | ").append(markdownValue(place == null ? null : place.placeName()))
                .append(" | ").append(markdownValue(place == null ? null : place.roadAddress()))
                .append(" | ").append(result)
                .append(" |\n");
    }

    private String markdownValue(String value) {
        return value == null ? "-" : value.replace("|", "\\|");
    }

    private Path findSampleDirectory() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return List.of(
                        workingDirectory.resolve("docs/receipt-ocr-samples-20260826"),
                        workingDirectory.resolve("..").resolve("docs/receipt-ocr-samples-20260826")
                ).stream()
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "영수증 OCR 샘플 디렉터리를 찾을 수 없습니다."
                ));
    }

    private void writeReport(Path reportPath, StringBuilder report) throws IOException {
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
    }

    private List<SearchCase> searchCases() {
        return List.of(
                new SearchCase(1, "유니클로", "서울 강남구 영동대로 513 코엑스몰 D1"),
                new SearchCase(2, "호시타코야끼 대치점", null),
                new SearchCase(3, "COS 현대코엑스점", "서울 강남구 테헤란로 517 현대백화점무역센터점 5층"),
                new SearchCase(4, "UNIQLO 스타필드 코엑스몰점", "서울시 강남구삼성동159 코엑스N12"),
                new SearchCase(5, "COS 현대 무역센터점", "서울 강남구 테헤란로 517, 현대백화점 5층"),
                new SearchCase(6, "광화문점", "서울특별시 종로구 종로1길 50 A동 1층(중학동)"),
                new SearchCase(7, "(주)현대백화점 압구정본점", "강남구 알구정료 165"),
                new SearchCase(8, "팀홀튼 광화문K-Twin타워점", "서울 종로구 종로1길 50 케이트윈타워 B동"),
                new SearchCase(9, "버터", "서울 강남구 영동대로 513 코엑스몰 D1"),
                new SearchCase(10, "주식회사 뎀에이치앤코 버터코엑", "서울특별시 강남구 영등대로 5"),
                new SearchCase(11, "송", "서울시 강남구 테헤란로 517"),
                new SearchCase(12, null, null),
                new SearchCase(13, "(주)신세계 SSG도곡", "서울특별시 강남구 도곡2동 467-32"),
                new SearchCase(14, "청년다방 (선릉역점)", "서울특별시 강남구 선릉로86길 31 1층"),
                new SearchCase(15, "공차 선릉중앙점", "서울특별시 강남구 선릉로86길 7"),
                new SearchCase(16, "공차 선릉중앙점", "서울특별시 강남구 선릉로86길 7"),
                new SearchCase(17, "이마트에브리데이 서초내곡점", "서울시서초구청계산로9길70(내곡동,내곡에스에이지품과지)"),
                new SearchCase(18, "153구포국수 (선릉역점)", "서울특별시 강남구 선릉로86길 8 (대치동)"),
                new SearchCase(19, "CJ올리브영(주) 코엑스몰점", "서울특별시 강남구 테헤란로87길 22"),
                new SearchCase(20, "첫걸음산부인과의원", "서울 강남구 선릉로 431 에스케이허브오피스텔 306 호")
        );
    }

    private record SearchCase(int receiptNumber, String storeName, String address) {
    }
}
