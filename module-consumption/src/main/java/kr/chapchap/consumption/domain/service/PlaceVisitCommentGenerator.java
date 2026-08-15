package kr.chapchap.consumption.domain.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class PlaceVisitCommentGenerator {

    private static final List<String> COUNT_1 = List.of("새로운 가게 발견! 지도에 점 하나 콕 찍었어요", "반가워요,오늘부터 아는 사이!", "첫 방문 기념,오늘의 첫인상 저장 완료!");
    private static final List<String> COUNT_2 = List.of("또 왔다! 우연은 아니죠?", "두 번 왔으면 이미 마음이 있는 거예요", "이 점,다시 찍힐 줄 알았어요!");
    private static final List<String> COUNT_3_TO_5 = List.of("사장님이 슬슬 알아볼 각!", "늘 먹던 걸로 주세요 시전 가능해졌어요", "이건 취향이 맞네요, 인정!");
    private static final List<String> COUNT_6_TO_8 = List.of("단골 등극! 이제 당당하게 말해도 돼요", "메뉴판? 안 봐도 다 알아요", "이 동네에서 제일 익숙한 문,오늘도 열었어요!");
    private static final List<String> COUNT_9_TO_11 = List.of("신메뉴 나오면 1등으로 달려갈 사람!", "사장님도 이제 기다리고 있을걸요?", "이 가게 분위기,절반은 내 지분이에요.");
    private static final List<String> COUNT_12_OR_MORE = List.of("이 가게 테이블 하나쯤은 내 자리예요.", "사실상 명예 직원! 유니폼만 없을 뿐.", "여기 역사에 내 이름 한 줄 있어야 해요.");

    public String generate(int monthlyVisitCount) {
        List<String> pool = resolvePool(monthlyVisitCount);
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private List<String> resolvePool(int monthlyVisitCount) {
        if (monthlyVisitCount <= 1) {
            return COUNT_1;
        }
        if (monthlyVisitCount == 2) {
            return COUNT_2;
        }
        if (monthlyVisitCount <= 5) {
            return COUNT_3_TO_5;
        }
        if (monthlyVisitCount <= 8) {
            return COUNT_6_TO_8;
        }
        if (monthlyVisitCount <= 11) {
            return COUNT_9_TO_11;
        }
        return COUNT_12_OR_MORE;
    }
}
