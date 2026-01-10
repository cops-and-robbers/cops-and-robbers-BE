package com.team.cops_and_robbers.auth.application;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomNicknameGenerator {

    private static final String[] ADJECTIVES = {
            "재빠른", "은밀한", "예리한", "노련한", "대담한",
            "침착한", "집요한", "영리한", "무모한", "냉철한",
            "치밀한", "민첩한", "똑똑한", "기민한", "철저한"
    };
    private static final String[] NOUNS = {
            "경찰", "형사", "수사관", "순경", "특공대",
            "보안관", "추적자", "감시자", "해결사",
            "도둑", "강도", "괴도", "용의자", "탈옥수",
            "범인", "밀수범", "사기꾼", "도망자", "침입자"
    };
    private static final int NICKNAME_SUFFIX_BOUND = 10_000;

    public String generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[random.nextInt(NOUNS.length)];
        int suffixNumber = random.nextInt(NICKNAME_SUFFIX_BOUND);

        return adjective + noun + suffixNumber;
    }
}
