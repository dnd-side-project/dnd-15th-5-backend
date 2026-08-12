package kr.chapchap.report.domain.service;


final class TimeOfDayWeightCalculator {

    static final int MORNING_START = 5;
    static final int AFTERNOON_START = 11;
    static final int EVENING_START = 17;
    static final int NIGHT_START = 21;

    static final int MORNING_WEIGHT = -2;
    static final int AFTERNOON_WEIGHT = -1;
    static final int EVENING_WEIGHT = 1;
    static final int NIGHT_WEIGHT = 2;

    private TimeOfDayWeightCalculator() {
    }

    static int hourWeight(int hour) {
        if (hour >= MORNING_START && hour < AFTERNOON_START) {
            return MORNING_WEIGHT;
        }
        if (hour >= AFTERNOON_START && hour < EVENING_START) {
            return AFTERNOON_WEIGHT;
        }
        if (hour >= EVENING_START && hour < NIGHT_START) {
            return EVENING_WEIGHT;
        }
        return NIGHT_WEIGHT;
    }
}
