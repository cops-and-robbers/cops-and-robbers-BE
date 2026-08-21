package com.team.cops_and_robbers.auth.application;

import com.team.cops_and_robbers.auth.domain.NicknameLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("랜덤 닉네임 생성기")
class RandomNicknameGeneratorTest {

    /** 무작위 값이라 한 번만 봐서는 규칙이 지켜졌는지 알 수 없어 반복해서 확인한다. */
    private static final int REPEAT_COUNT = 200;
    private static final int SUFFIX_LENGTH = 4;

    private final RandomNicknameGenerator generator = new RandomNicknameGenerator();

    @Test
    void 요청한_언어의_수식어와_동물로_생성한다() {
        assertSoftly(softly -> {
            for (NicknameLanguage language : NicknameLanguage.values()) {
                for (int i = 0; i < REPEAT_COUNT; i++) {
                    String nickname = generator.generate(language);
                    String withoutSuffix = nickname.substring(0, nickname.length() - SUFFIX_LENGTH);

                    softly.assertThat(language.adjectives())
                            .anyMatch(withoutSuffix::startsWith);
                    softly.assertThat(language.animals())
                            .anyMatch(withoutSuffix::endsWith);
                }
            }
        });
    }

    /** 패딩이 없으면 재빠른고양이7 처럼 1~3자리도 나온다. */
    @Test
    void 숫자는_항상_네_자리로_붙인다() {
        assertSoftly(softly -> {
            for (int i = 0; i < REPEAT_COUNT; i++) {
                String nickname = generator.generate(NicknameLanguage.KO);
                String suffix = nickname.substring(nickname.length() - SUFFIX_LENGTH);

                softly.assertThat(suffix).matches("\\d{4}");
            }
        });
    }

    @Test
    void 언어별로_다른_문자로_생성한다() {
        String korean = generator.generate(NicknameLanguage.KO);
        String japanese = generator.generate(NicknameLanguage.JA);
        String english = generator.generate(NicknameLanguage.EN);

        assertSoftly(softly -> {
            softly.assertThat(korean).matches("[가-힣]+\\d{4}");
            softly.assertThat(japanese).matches("[\\p{IsHiragana}\\p{IsKatakana}ー]+\\d{4}");
            softly.assertThat(english).matches("[A-Za-z]+\\d{4}");
        });
    }

    /** User.nickname이 30자라 세 언어 모두 넉넉히 들어가야 한다. */
    @Test
    void 닉네임이_스무_자를_넘지_않는다() {
        assertSoftly(softly -> {
            for (NicknameLanguage language : NicknameLanguage.values()) {
                for (int i = 0; i < REPEAT_COUNT; i++) {
                    softly.assertThat(generator.generate(language)).hasSizeLessThanOrEqualTo(20);
                }
            }
        });
    }
}
