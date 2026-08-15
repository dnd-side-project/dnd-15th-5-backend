package kr.chapchap.report.domain.entity;

import java.time.LocalTime;

public enum TimeSlot {

    DAWN("새벽"),
    MORNING("아침"),
    LUNCH("점심"),
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
        if (hour >= 2 && hour < 6) {
            return DAWN;
        }
        if (hour >= 6 && hour < 10) {
            return MORNING;
        }
        if (hour >= 10 && hour < 15) {
            return LUNCH;
        }
        if (hour >= 15 && hour < 20) {
            return EVENING;
        }
        return NIGHT;
    }
}
