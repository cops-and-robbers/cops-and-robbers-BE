package com.team.cops_and_robbers.play.distance.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.distance.application.dto.command.DistanceUpdateCommand;
import com.team.cops_and_robbers.play.distance.domain.ParticipantDistanceHash;
import com.team.cops_and_robbers.play.distance.repository.ParticipantDistanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ParticipantDistanceServiceTest extends ServiceUnitTest {

    private static final Long GAME_ID = 1L;
    private static final Long PARTICIPANT_ID = 10L;
    private static final Instant NOW = Instant.parse("2026-10-17T06:00:00Z");

    @InjectMocks
    private ParticipantDistanceService participantDistanceService;

    @Spy
    private Clock clock = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));

    @Mock
    private InGameParticipantCacheRepository inGameParticipantCacheRepository;

    @Mock
    private ParticipantDistanceRepository participantDistanceRepository;

    @Nested
    @DisplayName("이동 거리 수신")
    class UpdateDistance {

        @BeforeEach
        void 참가자로_인정한다() {
            given(inGameParticipantCacheRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(new InGameParticipantCache("달리는사람", Team.ROBBER, null)));
        }

        @Test
        void 첫_수신이면_그대로_저장된다() {
            given(participantDistanceRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.empty());

            participantDistanceService.updateDistance(command(120.0));

            assertThat(savedDistance()).isEqualTo(120.0);
        }

        @Test
        void 거리가_없으면_무시된다() {
            given(participantDistanceRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.empty());

            participantDistanceService.updateDistance(command(null));

            verify(participantDistanceRepository, never()).save(anyLong(), anyLong(), any());
        }

        @Test
        void 음수는_무시된다() {
            given(participantDistanceRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.empty());

            participantDistanceService.updateDistance(command(-1.0));

            verify(participantDistanceRepository, never()).save(anyLong(), anyLong(), any());
        }

        @Test
        void 직전_값보다_작으면_무시된다() {
            given(participantDistanceRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(previous(500.0, 10_000)));

            participantDistanceService.updateDistance(command(400.0));

            verify(participantDistanceRepository, never()).save(anyLong(), anyLong(), any());
        }

        @Test
        void 시속_25km를_넘는_증가는_무시된다() {
            given(participantDistanceRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(previous(500.0, 10_000)));

            participantDistanceService.updateDistance(command(700.0));

            verify(participantDistanceRepository, never()).save(anyLong(), anyLong(), any());
        }

        @Test
        void 시속_25km_이내의_증가는_저장된다() {
            given(participantDistanceRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(previous(500.0, 10_000)));

            participantDistanceService.updateDistance(command(550.0));

            assertThat(savedDistance()).isEqualTo(550.0);
        }

        private Double savedDistance() {
            ArgumentCaptor<ParticipantDistanceHash> captor = ArgumentCaptor.forClass(ParticipantDistanceHash.class);
            verify(participantDistanceRepository).save(eq(GAME_ID), eq(PARTICIPANT_ID), captor.capture());
            return captor.getValue().distanceMeters();
        }
    }

    @Test
    void 참가자가_아니면_예외가_발생한다() {
        given(inGameParticipantCacheRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> participantDistanceService.updateDistance(command(100.0)))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", GameParticipantException.NOT_A_PARTICIPANT);
    }

    private static DistanceUpdateCommand command(Double distanceMeters) {
        return new DistanceUpdateCommand(GAME_ID, PARTICIPANT_ID, distanceMeters);
    }

    private static ParticipantDistanceHash previous(Double distanceMeters, long millisAgo) {
        return new ParticipantDistanceHash(distanceMeters, NOW.toEpochMilli() - millisAgo);
    }
}
