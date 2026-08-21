package com.team.cops_and_robbers.auth.domain;

import java.util.Arrays;
import java.util.List;

/**
 * 자동 생성 닉네임의 언어별 낱말 목록.
 * <p>
 * 낱말이 직업이면 유능함이 전제라 "잠많은형사" 같은 조합이 성립하지 않고,
 * "사기꾼"처럼 자동으로 붙었을 때 모욕이 되는 것도 섞인다.
 * 동물은 어떤 수식어와도 붙고 세 언어가 1:1로 대응된다.
 * <p>
 * 세 언어의 낱말 수는 같아야 한다. 한쪽만 늘리면 언어에 따라 조합 수가 달라진다.
 */
public enum NicknameLanguage {

    KO(
            List.of("재빠른", "조용한", "느긋한", "살금살금", "유쾌한", "똑똑한", "용감한",
                    "엉뚱한", "다정한", "잠많은", "배고픈", "궁금한", "신난", "포근한",
                    "씩씩한", "부지런한", "수줍은", "늠름한", "반짝이는", "든든한", "나른한",
                    "상냥한", "부드러운", "발랄한", "진지한", "넉살좋은", "야무진", "통통한",
                    "두근대는", "싱그러운"),
            List.of("고양이", "쥐", "여우", "토끼", "다람쥐", "참새", "두더지", "오리", "곰", "햄스터",
                    "너구리", "수달", "고슴도치", "부엉이", "펭귄", "물개", "판다", "사슴", "늑대",
                    "거북이", "두루미", "고래", "돌고래", "코알라", "알파카")
    ),
    /** 동물만 카타카나로 쓴다. 전부 히라가나면 "ねむたいねずみ"처럼 낱말 경계가 보이지 않는다. */
    JA(
            List.of("すばやい", "しずかな", "のんびり", "こっそり", "ゆかいな", "かしこい", "ゆうかんな",
                    "ふしぎな", "やさしい", "ねむたい", "はらぺこ", "にこにこ", "げんきな", "おっとり",
                    "たくましい", "まめな", "はずかしがり", "りりしい", "きらきら", "たよれる", "ゆめみる",
                    "おだやかな", "ふわふわ", "はつらつ", "まじめな", "ひとなつこい", "しっかりもの", "まるまる",
                    "わくわく", "さわやかな"),
            List.of("ネコ", "ネズミ", "キツネ", "ウサギ", "リス", "スズメ", "モグラ", "アヒル", "クマ", "ハムスター",
                    "タヌキ", "カワウソ", "ハリネズミ", "フクロウ", "ペンギン", "アザラシ", "パンダ", "シカ", "オオカミ",
                    "カメ", "ツル", "クジラ", "イルカ", "コアラ", "アルパカ")
    ),
    EN(
            List.of("Speedy", "Quiet", "Sleepy", "Sneaky", "Cheerful", "Clever", "Brave",
                    "Curious", "Gentle", "Hungry", "Playful", "Cozy", "Lively", "Calm",
                    "Bold", "Busy", "Shy", "Noble", "Shiny", "Steady", "Dreamy",
                    "Kindly", "Fluffy", "Perky", "Serious", "Friendly", "Sharp", "Chubby",
                    "Eager", "Fresh"),
            List.of("Cat", "Mouse", "Fox", "Rabbit", "Squirrel", "Sparrow", "Mole", "Duck", "Bear", "Hamster",
                    "Raccoon", "Otter", "Hedgehog", "Owl", "Penguin", "Seal", "Panda", "Deer", "Wolf",
                    "Turtle", "Crane", "Whale", "Dolphin", "Koala", "Alpaca")
    );

    private static final NicknameLanguage DEFAULT = KO;

    private final List<String> adjectives;
    private final List<String> animals;

    NicknameLanguage(List<String> adjectives, List<String> animals) {
        this.adjectives = adjectives;
        this.animals = animals;
    }

    /** Accept-Language로 들어온 언어 코드를 매핑한다. 헤더가 없거나 지원하지 않는 언어면 한국어로 떨어뜨린다. */
    public static NicknameLanguage from(String languageCode) {
        return Arrays.stream(values())
                .filter(language -> language.name().equalsIgnoreCase(languageCode))
                .findFirst()
                .orElse(DEFAULT);
    }

    public List<String> adjectives() {
        return adjectives;
    }

    public List<String> animals() {
        return animals;
    }
}
