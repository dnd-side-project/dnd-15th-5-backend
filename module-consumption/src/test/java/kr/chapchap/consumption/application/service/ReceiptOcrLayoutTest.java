package kr.chapchap.consumption.application.service;

import kr.chapchap.consumption.application.info.ReceiptOcrDocument;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.Point;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.TextField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptOcrLayoutTest {

    @Test
    void 좌표가_역순이어도_CLOVA가_준_필드_순서를_유지한다() {
        // given
        ReceiptOcrDocument document = new ReceiptOcrDocument(List.of(
                field("합계", 300, false, 0.98, true),
                field("12,000원", 20, true, 0.99, true)
        ));

        // when
        ReceiptOcrLayout layout = ReceiptOcrLayout.from(document);

        // then
        assertThat(layout.lines()).extracting(ReceiptOcrLayout.Line::text)
                .containsExactly("합계 12,000원");
    }

    @Test
    void 신뢰도_평균에서_누락된_값을_영점으로_계산하지_않는다() {
        // given
        ReceiptOcrDocument document = new ReceiptOcrDocument(List.of(
                field("가맹점명", 20, false, 0.0, false),
                field("찹찹카페", 150, true, 0.8, true)
        ));

        // when
        ReceiptOcrLayout.Line line = ReceiptOcrLayout.from(document).lines().getFirst();

        // then
        assertThat(line.confidencePresent()).isTrue();
        assertThat(line.confidence()).isEqualTo(0.8);
        assertThat(line.hasConfidenceForRange(0, 4)).isFalse();
        assertThat(line.hasConfidenceForRange(5, 10)).isTrue();
    }

    private TextField field(
            String text,
            double left,
            boolean lineBreak,
            double confidence,
            boolean confidencePresent
    ) {
        return new TextField(
                text,
                confidence,
                lineBreak,
                List.of(
                        new Point(left, 20),
                        new Point(left + 100, 20),
                        new Point(left + 100, 40),
                        new Point(left, 40)
                ),
                confidencePresent
        );
    }
}
