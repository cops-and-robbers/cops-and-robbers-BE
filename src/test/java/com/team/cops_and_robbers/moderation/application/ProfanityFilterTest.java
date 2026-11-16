package com.team.cops_and_robbers.moderation.application;

import com.team.cops_and_robbers.moderation.infrastructure.BannedWordLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProfanityFilterTest {

    private static final ProfanityFilter profanityFilter = new ProfanityFilter(new BannedWordLoader());

    @Nested
    @DisplayName("마스킹")
    class Mask {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "씨발 뭐야|** 뭐야",
                "이 병신아|이 **아",
                "what the fuck|what the ****",
                "WHAT THE FUCK|WHAT THE ****",
                "クソ野郎め|****め"
        })
        void 금칙어를_같은_길이의_별표로_바꾼다(String text, String expected) {
            assertThat(profanityFilter.mask(text)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "시.발|***",
                "시1발|***",
                "시 발|* *",
                "ㅅㅣㅂㅏㄹ|*****",
                "ㅅㅂ 진짜|** 진짜",
                "ㅋㅋㅅㅂ|ㅋㅋ**",
                "sh1t|****",
                "a$$|***",
                "ｆｕｃｋ|****"
        })
        void 기호_숫자_공백_자모_전각으로_우회한_금칙어도_바꾼다(String text, String expected) {
            assertThat(profanityFilter.mask(text)).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "다들 어디야",
                "다시 발표할게요",
                "시발점에서 만나요",
                "시바견 키워요",
                "참여하고자 합니다",
                "유모차 끌고 갈게요",
                "폰이 망가졌어",
                "그거 보지 마",
                "class assassin",
                "costs $5 for 100"
        })
        void 금칙어가_우연히_들어간_정상_문장은_그대로_둔다(String text) {
            assertThat(profanityFilter.mask(text)).isEqualTo(text);
        }

        @Test
        void 허용_단어와_금칙어가_같이_있으면_금칙어만_바꾼다() {
            assertThat(profanityFilter.mask("시발 시발점")).isEqualTo("** 시발점");
        }

        @Test
        void 여러_언어의_금칙어가_섞여_있어도_모두_바꾼다() {
            assertThat(profanityFilter.mask("fuck 씨발 クソ野郎")).isEqualTo("**** ** ****");
        }

        @Test
        void null은_그대로_돌려준다() {
            assertThat(profanityFilter.mask(null)).isNull();
        }
    }

    @Nested
    @DisplayName("금칙어 포함 여부")
    class ContainsBannedWord {

        @Test
        void 금칙어가_있으면_true를_반환한다() {
            assertThat(profanityFilter.containsBannedWord("씨발경찰")).isTrue();
        }

        @Test
        void 금칙어가_없으면_false를_반환한다() {
            assertThat(profanityFilter.containsBannedWord("날렵한경찰123")).isFalse();
        }

        @Test
        void null이면_false를_반환한다() {
            assertThat(profanityFilter.containsBannedWord(null)).isFalse();
        }
    }
}
