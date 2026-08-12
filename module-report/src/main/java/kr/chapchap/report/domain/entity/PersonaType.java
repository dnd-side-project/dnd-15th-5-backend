package kr.chapchap.report.domain.entity;

import lombok.Getter;

import java.util.Arrays;


@Getter
public enum PersonaType {

    RHDP(VisitStyle.REGULAR, ActivityRange.HOME, ConsumptionTime.DAY, ConsumptionRhythm.PATTERN),
    RHDF(VisitStyle.REGULAR, ActivityRange.HOME, ConsumptionTime.DAY, ConsumptionRhythm.FREE),
    RHMP(VisitStyle.REGULAR, ActivityRange.HOME, ConsumptionTime.MOON, ConsumptionRhythm.PATTERN),
    RHMF(VisitStyle.REGULAR, ActivityRange.HOME, ConsumptionTime.MOON, ConsumptionRhythm.FREE),
    RWDP(VisitStyle.REGULAR, ActivityRange.WANDER, ConsumptionTime.DAY, ConsumptionRhythm.PATTERN),
    RWDF(VisitStyle.REGULAR, ActivityRange.WANDER, ConsumptionTime.DAY, ConsumptionRhythm.FREE),
    RWMP(VisitStyle.REGULAR, ActivityRange.WANDER, ConsumptionTime.MOON, ConsumptionRhythm.PATTERN),
    RWMF(VisitStyle.REGULAR, ActivityRange.WANDER, ConsumptionTime.MOON, ConsumptionRhythm.FREE),
    NHDP(VisitStyle.NOMAD, ActivityRange.HOME, ConsumptionTime.DAY, ConsumptionRhythm.PATTERN),
    NHDF(VisitStyle.NOMAD, ActivityRange.HOME, ConsumptionTime.DAY, ConsumptionRhythm.FREE),
    NHMP(VisitStyle.NOMAD, ActivityRange.HOME, ConsumptionTime.MOON, ConsumptionRhythm.PATTERN),
    NHMF(VisitStyle.NOMAD, ActivityRange.HOME, ConsumptionTime.MOON, ConsumptionRhythm.FREE),
    NWDP(VisitStyle.NOMAD, ActivityRange.WANDER, ConsumptionTime.DAY, ConsumptionRhythm.PATTERN),
    NWDF(VisitStyle.NOMAD, ActivityRange.WANDER, ConsumptionTime.DAY, ConsumptionRhythm.FREE),
    NWMP(VisitStyle.NOMAD, ActivityRange.WANDER, ConsumptionTime.MOON, ConsumptionRhythm.PATTERN),
    NWMF(VisitStyle.NOMAD, ActivityRange.WANDER, ConsumptionTime.MOON, ConsumptionRhythm.FREE),
    ;

    private final VisitStyle visitStyle;
    private final ActivityRange activityRange;
    private final ConsumptionTime consumptionTime;
    private final ConsumptionRhythm consumptionRhythm;

    PersonaType(VisitStyle visitStyle, ActivityRange activityRange, ConsumptionTime consumptionTime, ConsumptionRhythm consumptionRhythm) {
        this.visitStyle = visitStyle;
        this.activityRange = activityRange;
        this.consumptionTime = consumptionTime;
        this.consumptionRhythm = consumptionRhythm;
    }

    public static PersonaType of(VisitStyle visitStyle, ActivityRange activityRange,
                                  ConsumptionTime consumptionTime, ConsumptionRhythm consumptionRhythm) {
        return Arrays.stream(values())
                .filter(type -> type.visitStyle == visitStyle
                        && type.activityRange == activityRange
                        && type.consumptionTime == consumptionTime
                        && type.consumptionRhythm == consumptionRhythm)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("정의되지 않은 페르소나 조합입니다."));
    }

    public String getTypeName() {
        return visitStyle.label + " · " + activityRange.label + " · " + consumptionTime.label + " · " + consumptionRhythm.label;
    }

    public String getDescription() {
        return visitStyle.description + " " + activityRange.description + " "
                + consumptionTime.description + " " + consumptionRhythm.description;
    }


    @Getter
    public enum VisitStyle {
        REGULAR("단골 반복형", "익숙한 가게를 반복해서 찾는 편이에요."),
        NOMAD("신규 탐색형", "새로운 가게를 적극적으로 찾아 나서는 편이에요."),
        ;
        private final String label;
        private final String description;

        VisitStyle(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }


    @Getter
    public enum ActivityRange {
        HOME("동네 집중형", "익숙한 한 동네에 머무는 편이에요."),
        WANDER("동네 확장형", "여러 동네를 넘나드는 편이에요."),
        ;
        private final String label;
        private final String description;

        ActivityRange(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }


    @Getter
    public enum ConsumptionTime {
        DAY("낮소비형", "오전·오후에 소비하는 것을 즐기는 편이에요."),
        MOON("밤소비형", "저녁·밤에 소비하는 것을 즐기는 편이에요."),
        ;
        private final String label;
        private final String description;

        ConsumptionTime(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    @Getter
    public enum ConsumptionRhythm {
        PATTERN("규칙형", "정해진 요일에 규칙적으로 소비하는 편이에요."),
        FREE("즉흥형", "요일에 얽매이지 않고 즉흥적으로 소비하는 편이에요."),
        ;
        private final String label;
        private final String description;

        ConsumptionRhythm(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }
}
