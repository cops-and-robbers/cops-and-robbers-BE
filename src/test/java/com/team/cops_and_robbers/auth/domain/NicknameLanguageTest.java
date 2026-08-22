package com.team.cops_and_robbers.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class NicknameLanguageTest {

    @Nested
    @DisplayName("언어 코드 매핑")
    class From {

        @Test
        void 지원하는_언어_코드는_해당_언어를_반환한다() {
            assertSoftly(softly -> {
                softly.assertThat(NicknameLanguage.from("ko")).isEqualTo(NicknameLanguage.KO);
                softly.assertThat(NicknameLanguage.from("ja")).isEqualTo(NicknameLanguage.JA);
                softly.assertThat(NicknameLanguage.from("en")).isEqualTo(NicknameLanguage.EN);
            });
        }

        @Test
        void 대소문자가_달라도_해당_언어를_반환한다() {
            assertSoftly(softly -> {
                softly.assertThat(NicknameLanguage.from("KO")).isEqualTo(NicknameLanguage.KO);
                softly.assertThat(NicknameLanguage.from("Ja")).isEqualTo(NicknameLanguage.JA);
                softly.assertThat(NicknameLanguage.from("eN")).isEqualTo(NicknameLanguage.EN);
            });
        }

        @Test
        void 지원하지_않는_언어_코드는_한국어를_반환한다() {
            assertSoftly(softly -> {
                softly.assertThat(NicknameLanguage.from("fr")).isEqualTo(NicknameLanguage.KO);
                softly.assertThat(NicknameLanguage.from("zh")).isEqualTo(NicknameLanguage.KO);
            });
        }

        @Test
        void 언어_코드가_없으면_한국어를_반환한다() {
            assertSoftly(softly -> {
                softly.assertThat(NicknameLanguage.from(null)).isEqualTo(NicknameLanguage.KO);
                softly.assertThat(NicknameLanguage.from("")).isEqualTo(NicknameLanguage.KO);
            });
        }
    }

    @Nested
    @DisplayName("낱말 목록")
    class Words {

        private static final int ADJECTIVE_COUNT = 30;
        private static final int ANIMAL_COUNT = 25;

        @Test
        void 모든_언어가_같은_개수의_낱말을_가진다() {
            assertSoftly(softly -> {
                for (NicknameLanguage language : NicknameLanguage.values()) {
                    softly.assertThat(language.adjectives()).hasSize(ADJECTIVE_COUNT);
                    softly.assertThat(language.animals()).hasSize(ANIMAL_COUNT);
                }
            });
        }

        @Test
        void 낱말이_중복되지_않는다() {
            assertSoftly(softly -> {
                for (NicknameLanguage language : NicknameLanguage.values()) {
                    softly.assertThat(language.adjectives()).doesNotHaveDuplicates();
                    softly.assertThat(language.animals()).doesNotHaveDuplicates();
                }
            });
        }

        @Test
        void 일본어_동물은_카타카나로_표기한다() {
            assertThat(NicknameLanguage.JA.animals())
                    .allMatch(animal -> animal.chars().allMatch(NicknameLanguageTest::isKatakana));
        }
    }

    private static boolean isKatakana(int character) {
        return character >= 0x30A0 && character <= 0x30FF;
    }
}
