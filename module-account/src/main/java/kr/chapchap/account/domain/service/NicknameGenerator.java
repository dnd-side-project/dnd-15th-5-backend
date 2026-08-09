package kr.chapchap.account.domain.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "알뜰한",
            "꼼꼼한",
            "야무진",
            "계획적인",
            "기록하는",
            "탐험하는",
            "돌아보는",
            "꾸준한",
            "똑똑한",
            "슬기로운",
            "차분한",
            "다정한",
            "명랑한",
            "유쾌한",
            "용감한",
            "느긋한",
            "포근한",
            "든든한",
            "씩씩한",
            "반짝이는"
    );

    private static final List<String> ANIMALS = List.of(
            "토끼",
            "다람쥐",
            "수달",
            "고슴도치",
            "비버",
            "햄스터",
            "고양이",
            "강아지",
            "판다",
            "쿼카",
            "알파카",
            "펭귄",
            "코알라",
            "사슴",
            "참새",
            "부엉이",
            "오리",
            "미어캣",
            "돌고래",
            "북극곰"
    );

    public String generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String animal = ANIMALS.get(random.nextInt(ANIMALS.size()));

        return "%s %s".formatted(adjective, animal);
    }
}
