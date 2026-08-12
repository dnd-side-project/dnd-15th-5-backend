package kr.chapchap.report.domain.service;

import kr.chapchap.report.domain.entity.VisitActivity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecentDiscoveryMessageGeneratorTest {

    private final RecentDiscoveryMessageGenerator sut = new RecentDiscoveryMessageGenerator();

    @Test
    void 최근_최다_방문_동네가_이전과_다르면_생활권_변경_멘트를_만든다() {
        // given
        // 신규 장소 발견/시간대 멘트가 같이 후보로 뽑히지 않도록 placeId를 겹치게 하고 purchaseTime은 비워둔다.
        List<VisitActivity> recent = List.of(
                activity(1L, "신논현동", LocalDate.of(2026, 8, 8), null),
                activity(2L, "신논현동", LocalDate.of(2026, 8, 7), null),
                activity(3L, "신논현동", LocalDate.of(2026, 8, 6), null)
        );
        List<VisitActivity> previous = List.of(
                activity(1L, "연남동", LocalDate.of(2026, 7, 20), null),
                activity(2L, "연남동", LocalDate.of(2026, 7, 21), null),
                activity(3L, "연남동", LocalDate.of(2026, 7, 22), null)
        );

        // when
        String message = sut.generate(recent, previous);

        // then
        assertThat(message).isEqualTo("최근 연남동에서 신논현동으로 본거지를 이동중이시네요!");
    }

    @Test
    void 비교할_데이터가_충분하지_않으면_기본_멘트를_노출한다() {
        // given
        List<VisitActivity> recent = List.of(
                activity(1L, "신논현동", LocalDate.of(2026, 8, 8), LocalTime.of(12, 0))
        );
        List<VisitActivity> previous = List.of();

        // when
        String message = sut.generate(recent, previous);

        // then
        assertThat(message).isIn("이번 달 소비 기록을 꾸준히 남겨봐요", "오늘은 어떤 곳을 다녀오셨나요?");
    }

    @Test
    void 이전에_없던_장소를_임계값_이상_방문하면_신규_장소_발견_멘트를_만든다() {
        // given
        List<VisitActivity> recent = List.of(
                activity(101L, null, LocalDate.of(2026, 8, 8), null),
                activity(102L, null, LocalDate.of(2026, 8, 7), null)
        );
        List<VisitActivity> previous = List.of(
                activity(201L, null, LocalDate.of(2026, 7, 20), null)
        );

        // when
        String message = sut.generate(recent, previous);

        // then
        assertThat(message).isEqualTo("프로 단골러 등장! 최근 새로운 단골집이 2곳이나 늘어났어요");
    }

    private VisitActivity activity(Long placeId, String dongName, LocalDate date, LocalTime time) {
        return new VisitActivity(placeId, dongName, null, date, time);
    }
}
