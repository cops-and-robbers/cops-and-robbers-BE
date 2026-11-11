package com.team.cops_and_robbers.history.domain;

import com.team.cops_and_robbers.common.fixture.GameResultFixture;
import com.team.cops_and_robbers.common.fixture.GameResultParticipantFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GameResultParticipantTest {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(2);
    private static final LocalDateTime FIRST_ISSUED_AT = LocalDateTime.of(2026, 11, 9, 17, 0, 0);

    private GameResultParticipant police;

    @BeforeEach
    void setUp() {
        police = GameResultParticipantFixture.POLICE_PARTICIPANT(
                GameResultFixture.POLICE_WIN_RESULT(1L), 1L, "police"
        );
    }

    @Nested
    @DisplayName("무전 사용 시간 누적 (accumulateRadioSeconds)")
    class AccumulateRadioSeconds {

        @Test
        void 첫_발급이면_기준_시각만_남긴다() {
            // when
            police.accumulateRadioSeconds(FIRST_ISSUED_AT, TOKEN_TTL);

            // then
            assertSoftly(softly -> {
                softly.assertThat(police.getRadioSeconds()).isZero();
                softly.assertThat(police.getRadioLastIssuedAt()).isEqualTo(FIRST_ISSUED_AT);
            });
        }

        @Test
        void 직전_발급_이후_흐른_시간을_더한다() {
            // given
            police.accumulateRadioSeconds(FIRST_ISSUED_AT, TOKEN_TTL);
            LocalDateTime reissuedAt = FIRST_ISSUED_AT.plusSeconds(90);

            // when
            police.accumulateRadioSeconds(reissuedAt, TOKEN_TTL);

            // then
            assertSoftly(softly -> {
                softly.assertThat(police.getRadioSeconds()).isEqualTo(90);
                softly.assertThat(police.getRadioLastIssuedAt()).isEqualTo(reissuedAt);
            });
        }

        @Test
        void 토큰_수명을_넘긴_구간은_수명만큼만_더한다() {
            // given
            police.accumulateRadioSeconds(FIRST_ISSUED_AT, TOKEN_TTL);

            // when
            police.accumulateRadioSeconds(FIRST_ISSUED_AT.plusMinutes(10), TOKEN_TTL);

            // then
            assertSoftly(softly -> softly.assertThat(police.getRadioSeconds()).isEqualTo(120));
        }

        @Test
        void 직전_발급과_같은_시각이면_더하지_않는다() {
            // given
            police.accumulateRadioSeconds(FIRST_ISSUED_AT, TOKEN_TTL);

            // when
            police.accumulateRadioSeconds(FIRST_ISSUED_AT, TOKEN_TTL);

            // then
            assertSoftly(softly -> softly.assertThat(police.getRadioSeconds()).isZero());
        }
    }
}
