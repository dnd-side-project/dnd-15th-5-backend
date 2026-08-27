package kr.chapchap.report.domain.entity;

import java.time.LocalTime;

public enum TimeSlot {

    MORNING("오전"),
    LUNCH("오후"),
    EVENING("저녁"),
    NIGHT("밤");

    private final String label;

    TimeSlot(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TimeSlot from(LocalTime time) {
        return from(time.getHour());
    }

    public static TimeSlot from(int hour) {
        if (hour >= 5 && hour < 11) {
            return MORNING;
        }
        if (hour >= 11 && hour < 17) {
            return LUNCH;
        }
        if (hour >= 17 && hour < 21) {
            return EVENING;
        }
        return NIGHT;
    }
}
