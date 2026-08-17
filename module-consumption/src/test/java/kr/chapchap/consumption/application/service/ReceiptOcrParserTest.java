package kr.chapchap.consumption.application.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptOcrParserTest {

    private final ReceiptOcrParser receiptOcrParser = new ReceiptOcrParser();

    @Test
    void 영수증_문장에서_소비_정보를_추출한다() {
        // given
        List<String> lines = List.of(
                "영 수 증",
                "투썸플레이스 신논현점",
                "사업자등록번호 123-45-67890",
                "주소: 서울특별시 강남구 봉은사로 125 1층",
                "공급가액 30,000원",
                "부가세 3,000원",
                "승인일시 2026-07-25 11:20:00",
                "결제금액 33,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(lines);

        // then
        assertThat(result.storeName()).isEqualTo("투썸플레이스 신논현점");
        assertThat(result.address()).isEqualTo("서울특별시 강남구 봉은사로 125 1층");
        assertThat(result.purchaseDate()).isEqualTo(LocalDate.of(2026, 7, 25));
        assertThat(result.purchaseTime()).isEqualTo(LocalTime.of(11, 20));
        assertThat(result.amount()).isEqualTo(33_000L);
    }

    @Test
    void 라벨과_값이_다음_줄로_나뉘어도_정보를_추출한다() {
        // given
        List<String> lines = List.of(
                "상호명",
                "찹찹카페 강남점",
                "주소",
                "서울특별시 강남구",
                "테헤란로 123 2층",
                "거래일시",
                "2026.08.16 09:05",
                "합계",
                "54,000 원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(lines);

        // then
        assertThat(result.storeName()).isEqualTo("찹찹카페 강남점");
        assertThat(result.address()).isEqualTo("서울특별시 강남구 테헤란로 123 2층");
        assertThat(result.purchaseDate()).isEqualTo(LocalDate.of(2026, 8, 16));
        assertThat(result.purchaseTime()).isEqualTo(LocalTime.of(9, 5));
        assertThat(result.amount()).isEqualTo(54_000L);
    }

    @Test
    void 오전과_오후_표기가_있으면_12시간제를_24시간제로_변환한다() {
        // given
        List<String> afternoonLines = List.of("결제일시 2026-08-16 오후 1:20");
        List<String> midnightLines = List.of("결제일시 2026-08-16 오전 12:30");

        // when
        ReceiptOcrParser.ParsedReceipt afternoon = receiptOcrParser.parse(afternoonLines);
        ReceiptOcrParser.ParsedReceipt midnight = receiptOcrParser.parse(midnightLines);

        // then
        assertThat(afternoon.purchaseTime()).isEqualTo(LocalTime.of(13, 20));
        assertThat(midnight.purchaseTime()).isEqualTo(LocalTime.of(0, 30));
    }

    @Test
    void 영문_오전오후_표기가_시간_앞뒤에_있어도_24시간제로_변환한다() {
        // given
        List<String> afternoonLines = List.of("결제일시 2026-08-16 PM 1:20");
        List<String> midnightLines = List.of("결제일시 2026-08-16 12:30 AM");

        // when
        ReceiptOcrParser.ParsedReceipt afternoon = receiptOcrParser.parse(afternoonLines);
        ReceiptOcrParser.ParsedReceipt midnight = receiptOcrParser.parse(midnightLines);

        // then
        assertThat(afternoon.purchaseTime()).isEqualTo(LocalTime.of(13, 20));
        assertThat(midnight.purchaseTime()).isEqualTo(LocalTime.of(0, 30));
    }

    @Test
    void 오전오후_표기가_없으면_24시간제로_해석한다() {
        // given
        List<String> lines = List.of("결제일시 2026-08-16 13:20");

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(lines);

        // then
        assertThat(result.purchaseTime()).isEqualTo(LocalTime.of(13, 20));
    }

    @Test
    void 결제금액을_찾을_때_할인과_부가세를_제외한다() {
        // given
        List<String> lines = List.of(
                "찹찹식당",
                "할인합계 5,000원",
                "공급가액 30,000원",
                "부가세 3,000원",
                "총 결제금액 33,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(lines);

        // then
        assertThat(result.amount()).isEqualTo(33_000L);
    }

    @Test
    void 결제금액_뒤의_승인번호를_금액으로_인식하지_않는다() {
        // given
        List<String> lines = List.of(
                "찹찹식당",
                "총 결제금액 33 , 000원 할인 2,000원 승인번호 12345678"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(lines);

        // then
        assertThat(result.amount()).isEqualTo(33_000L);
    }

    @Test
    void 합계_다음_줄의_승인번호를_금액으로_인식하지_않는다() {
        // given
        List<String> lines = List.of(
                "찹찹식당",
                "합계",
                "승인번호 12345678"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(lines);

        // then
        assertThat(result.amount()).isNull();
    }

    @Test
    void 세_줄로_나뉜_도로명주소를_결합한다() {
        // given
        List<String> lines = List.of(
                "찹찹카페",
                "주소: 서울특별시",
                "강남구 테헤란로",
                "123 2층"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(lines);

        // then
        assertThat(result.address()).isEqualTo("서울특별시 강남구 테헤란로 123 2층");
    }

    @Test
    void 주소_라벨이_없어도_상호명을_주소에_포함하지_않는다() {
        // given
        List<String> lines = List.of(
                "찹찹카페",
                "서울특별시 강남구 봉은사로 125 1층",
                "결제금액 33,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(lines);

        // then
        assertThat(result.address()).isEqualTo("서울특별시 강남구 봉은사로 125 1층");
    }

    @Test
    void 일부_항목을_찾지_못하면_null로_반환한다() {
        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(List.of());

        // then
        assertThat(result.storeName()).isNull();
        assertThat(result.address()).isNull();
        assertThat(result.purchaseDate()).isNull();
        assertThat(result.purchaseTime()).isNull();
        assertThat(result.amount()).isNull();
    }
}
