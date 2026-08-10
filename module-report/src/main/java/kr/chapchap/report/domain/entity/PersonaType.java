package kr.chapchap.report.domain.entity;

import lombok.Getter;

@Getter
public enum PersonaType {

    NIGHT_PILGRIM("밤의 순례자", "정해진 동네, 익숙한 가게를 밤에 즐겨 찾는 편이에요."),
    NEW_EXPLORER("새로운 탐험가", "새로운 동네와 가게를 적극적으로 찾아 나서는 편이에요."),
    DAYTIME_SPENDER("낮의 생활자", "익숙한 동네에서 낮 시간대에 소비하는 것을 즐기는 편이에요."),
    ;

    private final String typeName;
    private final String description;

    PersonaType(String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
    }
}
