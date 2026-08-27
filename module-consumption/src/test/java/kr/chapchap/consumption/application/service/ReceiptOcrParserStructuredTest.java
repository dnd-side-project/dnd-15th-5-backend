package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.ReceiptOcrDocument;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.Point;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.TextField;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptOcrParserStructuredTest {

    private final ReceiptOcrParser receiptOcrParser = new ReceiptOcrParser();

    @Test
    void 매장_라벨의_값을_상단_로고보다_우선한다() {
        // given
        ReceiptOcrDocument document = document(
                field("Starfield", true, 210, 20, 390, 50),
                field("사업자번호: 104-81-91189", true, 30, 70, 310, 95),
                field("매장: 유니클로", true, 30, 120, 240, 150),
                field("구매 2025-10-03 16:47", true, 30, 400, 310, 430),
                field("합계 60,000", true, 30, 600, 260, 630)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.storeName()).isEqualTo("유니클로");
    }

    @Test
    void 여러_금액_열에서는_합계와_같은_행의_값을_선택한다() {
        // given
        ReceiptOcrDocument document = document(
                field("판매계", true, 30, 100, 130, 125),
                field("부가세", true, 30, 140, 130, 165),
                field("합계", true, 30, 180, 130, 205),
                field("143,273", true, 310, 100, 430, 125),
                field("14,327", true, 310, 140, 430, 165),
                field("157,600", true, 310, 180, 430, 205)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.amount()).isEqualTo(157_600L);
    }

    @Test
    void 한_행에_금액과_부가세가_함께_있으면_각_라벨에_가까운_값을_구분한다() {
        // given
        ReceiptOcrDocument document = document(
                field("[금액]", false, 20, 100, 100, 125),
                field("10,800", false, 130, 100, 230, 125),
                field("[부가세]", false, 280, 100, 370, 125),
                field("982", true, 410, 100, 470, 125)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.amount()).isEqualTo(10_800L);
    }

    @Test
    void 줄바꿈된_주소를_주소_문맥으로_결합한다() {
        // given
        ReceiptOcrDocument document = document(
                field("주소: 서울특별시 강남구 영동대로", true, 30, 100, 430, 125),
                field("513 코엑스몰 D1", true, 30, 132, 270, 157),
                field("TEL. 02-3453-5448", true, 30, 170, 270, 195)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.address())
                .isEqualTo("서울특별시 강남구 영동대로 513 코엑스몰 D1");
    }

    @Test
    void 대괄호_라벨과_압축된_날짜도_인식한다() {
        // given
        ReceiptOcrDocument document = document(
                field("[매장명] 빛담", true, 20, 40, 200, 65),
                field("[주소] 서울특별시 마포구 월드컵북로 123 2층", true, 20, 75, 520, 100),
                field("승인일 20240928", true, 20, 200, 260, 225),
                field("Total 4,100", true, 20, 260, 220, 285)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.storeName()).isEqualTo("빛담");
        assertThat(result.address()).isEqualTo("서울특별시 마포구 월드컵북로 123 2층");
        assertThat(result.purchaseDate()).isEqualTo(LocalDate.of(2024, 9, 28));
        assertThat(result.amount()).isEqualTo(4_100L);
    }

    @Test
    void 상태값과_식별번호만_있는_경우_상호명이나_금액으로_만들지_않는다() {
        // given
        ReceiptOcrDocument document = document(
                field("신용카드 매출전표", true, 20, 30, 260, 55),
                field("픽업번호 A-13", true, 20, 70, 220, 95),
                field("테이블명 12", true, 20, 105, 190, 130),
                field("정상승인", true, 20, 140, 150, 165),
                field("승인번호 12345678", true, 20, 175, 280, 200),
                field("승인일시 2026-08-27 12:30", true, 20, 210, 360, 235)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.storeName()).isNull();
        assertThat(result.amount()).isNull();
    }

    @Test
    void 카드_매출전표의_결제액을_할인_전_합계보다_우선한다() {
        // given
        ReceiptOcrDocument document = document(
                field("합계", true, 20, 100, 100, 125),
                field("12,000", true, 300, 100, 400, 125),
                field("받을금액", true, 20, 135, 140, 160),
                field("12,000", true, 300, 135, 400, 160),
                field("신용카드 매출전표 [고객용]", true, 20, 200, 300, 225),
                field("[금액]", false, 20, 240, 100, 265),
                field("10,800 원(일시불)", true, 130, 240, 330, 265)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.amount()).isEqualTo(10_800L);
    }

    @Test
    void 상품표의_금액_헤더는_명확한_총합계보다_우선하지_않는다() {
        // given
        ReceiptOcrDocument document = document(
                field("[상품명]", false, 20, 100, 120, 125),
                field("[수량]", false, 170, 100, 250, 125),
                field("[금액]", true, 300, 100, 380, 125),
                field("아메리카노", false, 20, 140, 150, 165),
                field("1", false, 190, 140, 205, 165),
                field("5,000", true, 300, 140, 380, 165),
                field("총합계", false, 20, 220, 110, 245),
                field("12,000", true, 300, 220, 400, 245)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.amount()).isEqualTo(12_000L);
    }

    @Test
    void 승인번호는_근처의_합계와_상관없이_금액에서_제외한다() {
        // given
        ReceiptOcrDocument document = document(
                field("합계", true, 20, 100, 100, 125),
                field("승인No:", false, 20, 135, 130, 160),
                field("75100652", true, 160, 135, 300, 160),
                field("승인금액", false, 20, 180, 140, 205),
                field("17,300", true, 170, 180, 270, 205)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.amount()).isEqualTo(17_300L);
    }

    @Test
    void 분리된_영문_로고와_지점명을_결합한다() {
        // given
        ReceiptOcrDocument document = document(
                field("UNI", true, 220, 20, 320, 50),
                field("QLO", true, 220, 55, 325, 85),
                field("스타필드 코엑스몰점", true, 20, 100, 280, 125),
                field("2025-10-03 16:47", true, 20, 240, 260, 265),
                field("합계 60,000", true, 20, 300, 220, 325)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.storeName()).isEqualTo("UNIQLO 스타필드 코엑스몰점");
    }

    @Test
    void 필드_순서와_좌표_순서가_달라도_상단_영문_로고를_찾는다() {
        // given
        ReceiptOcrDocument document = document(
                field("합계 60,000", true, 20, 300, 220, 325),
                field("UNI", true, 220, 20, 320, 50),
                field("QLO", true, 220, 55, 325, 85),
                field("스타필드 코엑스몰점", true, 20, 100, 280, 125),
                field("2025-10-03 16:47", true, 20, 240, 260, 265)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.storeName()).isEqualTo("UNIQLO 스타필드 코엑스몰점");
    }

    @Test
    void 낮은_신뢰도의_한_글자_가맹점명과_상품행은_상호명으로_선택하지_않는다() {
        // given
        ReceiptOcrDocument document = document(
                field("가맹점명:", false, 20, 20, 140, 45),
                field("r", true, 150, 20, 165, 45, 0.43),
                field("수량", true, 20, 100, 80, 125),
                field("할인", true, 100, 100, 160, 125),
                field("금액", true, 180, 100, 240, 125),
                field("아메리카노", true, 20, 135, 160, 160),
                field("10,000", true, 300, 135, 400, 160),
                field("결제금액 10,000", true, 20, 220, 260, 245)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.storeName()).isNull();
    }

    @Test
    void 신뢰도가_누락되어도_라벨_상호명을_거부하지_않는다() {
        // given
        ReceiptOcrDocument document = document(
                fieldWithoutConfidence("가맹점명: 찹찹카페", true, 20, 30, 250, 55),
                field("결제일시 2026-08-27 12:30", true, 20, 100, 350, 125),
                field("결제금액 12,000", true, 20, 150, 280, 175)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.storeName()).isEqualTo("찹찹카페");
    }

    @Test
    void 상세주소가_이어지는_다음_행의_갈라진_건물번호를_결합한다() {
        // given
        ReceiptOcrDocument document = document(
                field(
                        "주소: 서울특별시 종로구 종로1길 5",
                        true,
                        20,
                        100,
                        520,
                        125
                ),
                field("0. A동 1층(중학동)", true, 20, 130, 190, 155),
                field("대표자: 주혜윤", true, 20, 170, 220, 195)
        );

        // when
        ReceiptOcrParser.ParsedReceipt result = receiptOcrParser.parse(document);

        // then
        assertThat(result.address())
                .isEqualTo("서울특별시 종로구 종로1길 50 A동 1층(중학동)");
    }

    private ReceiptOcrDocument document(TextField... fields) {
        return new ReceiptOcrDocument(List.of(fields));
    }

    private TextField field(
            String text,
            boolean lineBreak,
            double left,
            double top,
            double right,
            double bottom
    ) {
        return field(text, lineBreak, left, top, right, bottom, 0.98);
    }

    private TextField field(
            String text,
            boolean lineBreak,
            double left,
            double top,
            double right,
            double bottom,
            double confidence
    ) {
        return new TextField(
                text,
                confidence,
                lineBreak,
                List.of(
                        new Point(left, top),
                        new Point(right, top),
                        new Point(right, bottom),
                        new Point(left, bottom)
                )
        );
    }

    private TextField fieldWithoutConfidence(
            String text,
            boolean lineBreak,
            double left,
            double top,
            double right,
            double bottom
    ) {
        return new TextField(
                text,
                0.0,
                lineBreak,
                List.of(
                        new Point(left, top),
                        new Point(right, top),
                        new Point(right, bottom),
                        new Point(left, bottom)
                ),
                false
        );
    }
}
