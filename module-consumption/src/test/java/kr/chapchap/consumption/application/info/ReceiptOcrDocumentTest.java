package kr.chapchap.consumption.application.info;

import kr.chapchap.consumption.application.info.ReceiptOcrDocument.Point;
import kr.chapchap.consumption.application.info.ReceiptOcrDocument.TextField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptOcrDocumentTest {

    @Test
    void 필드와_좌표를_방어적으로_복사한다() {
        // given
        List<Point> vertices = new ArrayList<>(List.of(new Point(10.0, 20.0)));
        TextField field = new TextField("합계", 0.99, true, vertices);
        List<TextField> fields = new ArrayList<>(List.of(field));

        // when
        ReceiptOcrDocument document = new ReceiptOcrDocument(fields);
        vertices.clear();
        fields.clear();

        // then
        assertThat(document.fields()).containsExactly(field);
        assertThat(document.fields().getFirst().boundingVertices())
                .containsExactly(new Point(10.0, 20.0));
    }

    @Test
    void lineBreak에_따라_필드를_라인으로_재구성한다() {
        // given
        ReceiptOcrDocument document = new ReceiptOcrDocument(List.of(
                new TextField("결제금액", 0.98, false, List.of()),
                new TextField("12,000원", 0.99, true, List.of()),
                new TextField("승인", 0.97, true, List.of())
        ));

        // when
        List<String> lines = document.lines();

        // then
        assertThat(lines).containsExactly("결제금액 12,000원", "승인");
    }

    @Test
    void 문자열_라인은_OCR_메타데이터가_없는_문서로_변환한다() {
        // when
        ReceiptOcrDocument document = ReceiptOcrDocument.fromLines(List.of(
                "합계 12,000원",
                " ",
                "승인"
        ));

        // then
        assertThat(document.lines()).containsExactly("합계 12,000원", "승인");
        assertThat(document.fields())
                .allSatisfy(field -> {
                    assertThat(field.confidencePresent()).isFalse();
                    assertThat(field.boundingVertices()).isEmpty();
                });
    }
}
