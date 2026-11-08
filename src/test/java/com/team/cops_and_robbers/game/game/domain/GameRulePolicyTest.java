package com.team.cops_and_robbers.game.game.domain;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.exception.GameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.team.cops_and_robbers.common.fixture.GameFixture.EVENT_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameRulePolicyTest {

    private final GameRulePolicy gameRulePolicy = new GameRulePolicy();

    @Nested
    @DisplayName("게임 규칙 결정")
    class Rules {

        @Test
        void 일반_게임은_경찰과도둑_규칙을_따른다() {
            assertThat(IN_PROGRESS_GAME().rules()).isEqualTo(GameRules.COPS_AND_ROBBERS);
        }

        @Test
        void 이벤트_게임은_이벤트_규칙을_따른다() {
            assertThat(EVENT_GAME().rules()).isEqualTo(GameRules.EVENT);
        }
    }

    @Nested
    @DisplayName("강제 체포 검증")
    class ValidateForceArrest {

        @Test
        void 경찰과도둑_규칙이면_통과한다() {
            assertThatCode(() -> gameRulePolicy.validateForceArrest(IN_PROGRESS_GAME()))
                    .doesNotThrowAnyException();
        }

        @Test
        void 이벤트_규칙이면_거절한다() {
            assertThatThrownBy(() -> gameRulePolicy.validateForceArrest(EVENT_GAME()))
                    .isInstanceOf(ApplicationException.class)
                    .hasFieldOrPropertyWithValue("code", GameException.FORCE_ARREST_NOT_ALLOWED_IN_EVENT_GAME);
        }
    }
}
