package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.ReceiptOcrDocument;
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
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.storeName()).isEqualTo("투썸플레이스 신논현점");
        assertThat(result.address()).isEqualTo("서울특별시 강남구 봉은사로 125 1층");
        assertThat(result.purchaseDate()).isEqualTo(LocalDate.of(2026, 7, 25));
        assertThat(result.purchaseTime()).isEqualTo(LocalTime.of(11, 20));
        assertThat(result.amount()).isEqualTo(33_000L);
    }

    @Test
    void OCR_결과가_적어도_영수증_표시_직후의_상호명은_보존한다() {
        // given
        List<String> lines = List.of(
                "[영수증]",
                "울프혜라의 낭만캠핑"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.storeName()).isEqualTo("울프혜라의 낭만캠핑");
    }

    @Test
    void 영수증_표시와_떨어진_담당자값을_상호명으로_선택하지_않는다() {
        // given
        List<String> lines = List.of(
                "[영수증]",
                "20190611-01-0018",
                "01-서대경",
                "합계금액 10,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.storeName()).isNull();
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
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

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
        ReceiptOcrParser.ParsedReceipt afternoon = parse(afternoonLines);
        ReceiptOcrParser.ParsedReceipt midnight = parse(midnightLines);

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
        ReceiptOcrParser.ParsedReceipt afternoon = parse(afternoonLines);
        ReceiptOcrParser.ParsedReceipt midnight = parse(midnightLines);

        // then
        assertThat(afternoon.purchaseTime()).isEqualTo(LocalTime.of(13, 20));
        assertThat(midnight.purchaseTime()).isEqualTo(LocalTime.of(0, 30));
    }

    @Test
    void 오전오후_표기가_없으면_24시간제로_해석한다() {
        // given
        List<String> lines = List.of("결제일시 2026-08-16 13:20");

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

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
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

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
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

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
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

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
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

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
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isEqualTo("서울특별시 강남구 봉은사로 125 1층");
    }

    @Test
    void 도로명_OCR이_깨져도_구와_건물번호가_남으면_주소_후보로_반환한다() {
        // given
        List<String> lines = List.of(
                "(주)현대백화점 압구정본점",
                "강남구 알구정료 165",
                "사업자번호 211-85-37633",
                "합계 13,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isEqualTo("강남구 알구정료 165");
    }

    @Test
    void 주소의_건물번호가_갈려도_좌표_근거가_없으면_숫자를_추측하지_않는다() {
        // given
        List<String> lines = List.of(
                "주소: 서울특별시 종로구 종로1길 5",
                "0. A동 1층(중학동)",
                "대표자: 주혜윤"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address())
                .isEqualTo("서울특별시 종로구 종로1길 5 0 A동 1층(중학동)");
    }

    @Test
    void 명시적인_구매매장_라벨_뒤의_한_글자_상호명은_보존한다() {
        // given
        List<String> lines = List.of(
                "현대백화점 무역센터점",
                "구매매장 (교환/환불 문의)",
                "송",
                "합계 36,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.storeName()).isEqualTo("송");
    }

    @Test
    void 주소_단어가_줄_경계에서_갈리면_주소_복합어를_복원한다() {
        // given
        List<String> departmentStoreLines = List.of(
                "주소: 서울 강남구 테헤란로 517 현대백",
                "화점무역센터점 5층",
                "TEL: 070-4166-8931"
        );
        List<String> towerLines = List.of(
                "매장주소: 서울 종로구 종로1길 50 케이트윈타",
                "워 B동",
                "사업자번호: 101-86-76277"
        );

        // when
        ReceiptOcrParser.ParsedReceipt departmentStore = parse(
                departmentStoreLines
        );
        ReceiptOcrParser.ParsedReceipt tower = parse(towerLines);

        // then
        assertThat(departmentStore.address())
                .isEqualTo("서울 강남구 테헤란로 517 현대백화점무역센터점 5층");
        assertThat(tower.address())
                .isEqualTo("서울 종로구 종로1길 50 케이트윈타워 B동");
    }

    @Test
    void 정상_도로명주소를_주소로_추출한다() {
        // given
        List<String> lines = List.of(
                "(주)현대백화점 압구정본점",
                "강남구 압구정로 165",
                "사업자번호 211-85-37633",
                "합계 13,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isEqualTo("강남구 압구정로 165");
    }

    @Test
    void 정상_주소와_깨진_주소가_동시에_있으면_정상_주소를_우선한다() {
        // given
        List<String> lines = List.of(
                "강남구 압구정로 165",
                "강남구 알구정료 165",
                "사업자번호 211-85-37633",
                "합계 13,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isEqualTo("강남구 압구정로 165");
    }

    @Test
    void 도로명에_지역명이_포함돼도_주소_앞부분을_잘라내지_않는다() {
        // given
        List<String> lines = List.of(
                "주소: 서울특별시 동대문구 서울시립대로 163",
                "사업자번호 123-45-67890"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isEqualTo("서울특별시 동대문구 서울시립대로 163");
    }

    @Test
    void 주소처럼_보이는_일반_문장을_근거_없이_주소로_반환하지_않는다() {
        // given
        List<String> lines = List.of(
                "구입시 주문번호 123",
                "합계 12,000원",
                "결제일시 2026-08-27 12:30"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isNull();
    }

    @Test
    void 주소_라벨_뒤의_무관한_문장을_주소_앞에_붙이지_않는다() {
        // given
        List<String> lines = List.of(
                "주소",
                "찹찹카페",
                "서울 강남구 테헤란로 1",
                "사업자번호 123-45-67890"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isEqualTo("서울 강남구 테헤란로 1");
    }

    @Test
    void 완성된_주소_다음의_상품_숫자를_주소에_붙이지_않는다() {
        // given
        List<String> lines = List.of(
                "서울특별시 강남구 선릉로86길 7",
                "0",
                "1개",
                "합계 11,700원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isEqualTo("서울특별시 강남구 선릉로86길 7");
    }

    @Test
    void 주소_다음의_안내문과_전화번호를_주소에_붙이지_않는다() {
        // given
        List<String> completeAddressLines = List.of(
                "서울시서초구청계산로9길70(내곡동)",
                "구매시 포인트 적립",
                "영수증 미지참시 교환/환불 불가"
        );
        List<String> incompleteAddressLines = List.of(
                "서울특별시 강남구",
                "2 TEL: (02) 0000-0000"
        );

        // when
        ReceiptOcrParser.ParsedReceipt completeAddress = parse(
                completeAddressLines
        );
        ReceiptOcrParser.ParsedReceipt incompleteAddress = parse(
                incompleteAddressLines
        );

        // then
        assertThat(completeAddress.address()).isEqualTo("서울시서초구청계산로9길70(내곡동)");
        assertThat(incompleteAddress.address()).isNull();
    }

    @Test
    void 주소_다음의_영수증_안내를_상세주소로_오인하지_않는다() {
        // given
        List<String> lines = List.of(
                "주소: 서울 강남구 테헤란로 1",
                "영수증번호 1234",
                "영수증 보관"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isEqualTo("서울 강남구 테헤란로 1");
    }

    @Test
    void 적립시_안내문을_행정구역이_포함된_주소로_오인하지_않는다() {
        // given
        List<String> lines = List.of(
                "적립시 포인트 1000",
                "사업자번호 123-45-67890",
                "합계 12,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.address()).isNull();
    }

    @Test
    void 판매영수증_표시는_제거하고_브랜드와_지점명은_보존한다() {
        // given
        List<String> lines = List.of(
                "Tim Hortons.",
                "[판매영수증] 팀홀튼 광화문K-Twin타워점",
                "결제일시:2025-10-05 12:39:00",
                "매장주소: 서울 종로구 종로1길 50 케이트윈타워 B동",
                "결제금액 23,200"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.storeName()).isEqualTo("팀홀튼 광화문K-Twin타워점");
    }

    @Test
    void 사업자_근처의_회사_상호명을_영문_브랜드_로고보다_우선한다() {
        // given
        List<String> lines = List.of(
                "SHINSEGAE",
                "FOOD MARKET",
                "(주)신세계 SSG도곡",
                "T.02-1588-1234",
                "서울특별시 강남구 도곡2동",
                "467-32",
                "201-81-32195 박주형",
                "구매 2025-10-02 13:46",
                "카드결제액 100,000"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.storeName()).isEqualTo("(주)신세계 SSG도곡");
    }

    @Test
    void 결제대행사와_이름이_겹치는_명시적_브랜드는_상호명으로_보존한다() {
        // given
        List<String> vansLines = List.of(
                "매장명: VANS 강남점",
                "결제일시 2026-08-27 12:30",
                "합계 12,000원"
        );
        List<String> niceLines = List.of(
                "매장명: NICE WEATHER",
                "결제일시 2026-08-27 12:30",
                "합계 12,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt vans = parse(vansLines);
        ReceiptOcrParser.ParsedReceipt nice = parse(niceLines);

        // then
        assertThat(vans.storeName()).isEqualTo("VANS 강남점");
        assertThat(nice.storeName()).isEqualTo("NICE WEATHER");
    }

    @Test
    void 포인트와_이용가능_지점_문구를_상호명으로_선택하지_않는다() {
        // given
        List<String> lines = List.of(
                "거래일시 2026-08-27 12:30",
                "합계 12,000원",
                "18점",
                "사용 가능 지점"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.storeName()).isNull();
    }

    @Test
    void 승인번호의_8자리_숫자를_구매일로_선택하지_않는다() {
        // given
        List<String> lines = List.of(
                "승인번호 20260827",
                "거래일시 2026-08-26 12:30",
                "결제금액 12,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.purchaseDate()).isEqualTo(LocalDate.of(2026, 8, 26));
    }

    @Test
    void 할인합계와_부가세합계를_최종_결제금액으로_선택하지_않는다() {
        // given
        List<String> lines = List.of(
                "합계 30,000원",
                "할인합계 5,000원",
                "할인총액 4,000원",
                "쿠폰합계 3,000원",
                "쿠폰사용합계 2,500원",
                "쿠폰금액 2,400원",
                "포인트총액 2,000원",
                "포인트적립금액 1,000원",
                "부가세합계 2,727원",
                "TAX TOTAL 2,727원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.amount()).isEqualTo(30_000L);
    }

    @Test
    void 상품표의_금액과_부가세보다_총합계를_우선한다() {
        // given
        List<String> lines = List.of(
                "[상품명] [수량] [금액] 5,000원 [부가세] 455원",
                "총합계 12,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.amount()).isEqualTo(12_000L);
    }

    @Test
    void 결제대상금액과_최종금액을_일반_합계보다_우선한다() {
        // given
        List<String> settlementLines = List.of(
                "합계 12,000원",
                "결제대상금액 10,800원"
        );
        List<String> finalLines = List.of(
                "총합계 12,000원",
                "최종금액 10,800원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt settlement = parse(settlementLines);
        ReceiptOcrParser.ParsedReceipt finalAmount = parse(finalLines);

        // then
        assertThat(settlement.amount()).isEqualTo(10_800L);
        assertThat(finalAmount.amount()).isEqualTo(10_800L);
    }

    @Test
    void 받은_현금보다_명시적인_합계를_우선한다() {
        // given
        List<String> lines = List.of(
                "합계 10,000원",
                "현금",
                "20,000원",
                "거스름돈 10,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.amount()).isEqualTo(10_000L);
    }

    @Test
    void 영문_전표_헤더를_상호명으로_선택하지_않는다() {
        // given
        List<String> lines = List.of(
                "RECEIPT",
                "TAX INVOICE",
                "CUSTOMER COPY",
                "ORIGINAL RECEIPT",
                "CREDIT CARD RECEIPT",
                "THANK YOU",
                "사업자번호 123-45-67890",
                "거래일시 2026-08-27 12:30",
                "TOTAL 12,000원"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.storeName()).isNull();
    }

    @Test
    void 결제수단_다음의_서식_없는_식별번호를_금액으로_선택하지_않는다() {
        // given
        List<String> lines = List.of(
                "합계 30,000원",
                "신용카드",
                "12345678"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.amount()).isEqualTo(30_000L);
    }

    @Test
    void 영문_SUBTOTAL을_TOTAL로_오인하지_않는다() {
        // given
        List<String> lines = List.of(
                "SUBTOTAL 10,000",
                "TOTAL 12,000"
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = parse(lines);

        // then
        assertThat(result.amount()).isEqualTo(12_000L);
    }

    @Test
    void 일부_항목을_찾지_못하면_null로_반환한다() {
        // when
        ReceiptOcrParser.ParsedReceipt result = parse(List.of());

        // then
        assertThat(result.storeName()).isNull();
        assertThat(result.address()).isNull();
        assertThat(result.purchaseDate()).isNull();
        assertThat(result.purchaseTime()).isNull();
        assertThat(result.amount()).isNull();
    }

    private ReceiptOcrParser.ParsedReceipt parse(List<String> lines) {
        return receiptOcrParser.parse(ReceiptOcrDocument.fromLines(lines));
    }
}
