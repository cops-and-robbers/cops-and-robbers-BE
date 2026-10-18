package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TeamMvpSelectorTest extends ServiceUnitTest {

    @InjectMocks
    private TeamMvpSelector teamMvpSelector;

    @Nested
    @DisplayName("경찰 MVP 판정")
    class SelectPoliceMvp {

        @Test
        void 체포_횟수가_가장_많은_경찰이_선정된다() {
            List<GameResultParticipant> participants = List.of(
                    police(1L, 1, 100.0),
                    police(2L, 3, 100.0),
                    police(3L, 0, 100.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectPoliceMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 체포_횟수가_같으면_이동_거리가_긴_경찰이_선정된다() {
            List<GameResultParticipant> participants = List.of(
                    police(1L, 2, 500.0),
                    police(2L, 2, 1500.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectPoliceMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 체포_횟수와_거리가_모두_같으면_먼저_참가한_경찰이_선정된다() {
            List<GameResultParticipant> participants = List.of(
                    police(7L, 2, 1000.0),
                    police(3L, 2, 1000.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectPoliceMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(3L);
        }

        @Test
        void 전원_체포_0회여도_한_명이_선정된다() {
            List<GameResultParticipant> participants = List.of(
                    police(1L, 0, 300.0),
                    police(2L, 0, 900.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectPoliceMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 거리를_받지_못한_참가자는_0으로_본다() {
            List<GameResultParticipant> participants = List.of(
                    police(1L, 0, null),
                    police(2L, 0, 10.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectPoliceMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 중도_퇴장자는_체포_횟수가_가장_많아도_제외된다() {
            List<GameResultParticipant> participants = List.of(
                    policeLeft(1L, 9),
                    police(2L, 1, 100.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectPoliceMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 팀원이_전원_퇴장하면_MVP가_없다() {
            List<GameResultParticipant> participants = List.of(
                    policeLeft(1L, 3),
                    policeLeft(2L, 1)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectPoliceMvp(participants);

            assertThat(mvp).isEmpty();
        }

        @Test
        void 도둑은_경찰_MVP_후보가_아니다() {
            List<GameResultParticipant> participants = List.of(
                    robber(1L, 0, ParticipantStatus.ALIVE, 5000.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectPoliceMvp(participants);

            assertThat(mvp).isEmpty();
        }
    }

    @Nested
    @DisplayName("도둑 MVP 판정")
    class SelectRobberMvp {

        @Test
        void 잡힌_횟수가_가장_적은_도둑이_선정된다() {
            List<GameResultParticipant> participants = List.of(
                    robber(1L, 2, ParticipantStatus.ALIVE, 100.0),
                    robber(2L, 0, ParticipantStatus.ALIVE, 100.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectRobberMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 잡힌_횟수가_같으면_감옥_밖_도둑이_선정된다() {
            List<GameResultParticipant> participants = List.of(
                    robber(1L, 1, ParticipantStatus.JAILED, 2000.0),
                    robber(2L, 1, ParticipantStatus.ALIVE, 100.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectRobberMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 잡힌_횟수와_감옥_여부가_같으면_이동_거리가_긴_도둑이_선정된다() {
            List<GameResultParticipant> participants = List.of(
                    robber(1L, 1, ParticipantStatus.JAILED, 300.0),
                    robber(2L, 1, ParticipantStatus.JAILED, 1200.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectRobberMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 모든_기준이_같으면_먼저_참가한_도둑이_선정된다() {
            List<GameResultParticipant> participants = List.of(
                    robber(5L, 1, ParticipantStatus.ALIVE, 800.0),
                    robber(2L, 1, ParticipantStatus.ALIVE, 800.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectRobberMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 전원_잡힌_횟수가_0이어도_한_명이_선정된다() {
            List<GameResultParticipant> participants = List.of(
                    robber(1L, 0, ParticipantStatus.ALIVE, 100.0),
                    robber(2L, 0, ParticipantStatus.ALIVE, 700.0)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectRobberMvp(participants);

            assertThat(mvp).get().extracting(GameResultParticipant::getId).isEqualTo(2L);
        }

        @Test
        void 팀원이_전원_퇴장하면_MVP가_없다() {
            List<GameResultParticipant> participants = List.of(
                    robberLeft(1L, 0),
                    robberLeft(2L, 1)
            );

            Optional<GameResultParticipant> mvp = teamMvpSelector.selectRobberMvp(participants);

            assertThat(mvp).isEmpty();
        }
    }

    private GameResultParticipant police(Long id, int arrestCount, Double distanceMeters) {
        return participant(id, Team.POLICE, arrestCount, 0, ParticipantStatus.ALIVE, distanceMeters, null);
    }

    private GameResultParticipant policeLeft(Long id, int arrestCount) {
        return participant(id, Team.POLICE, arrestCount, 0, ParticipantStatus.ALIVE, 1000.0,
                LocalDateTime.of(2026, 10, 17, 12, 0));
    }

    private GameResultParticipant robber(Long id, int arrestedCount, ParticipantStatus status, Double distanceMeters) {
        return participant(id, Team.ROBBER, 0, arrestedCount, status, distanceMeters, null);
    }

    private GameResultParticipant robberLeft(Long id, int arrestedCount) {
        return participant(id, Team.ROBBER, 0, arrestedCount, ParticipantStatus.ALIVE, 1000.0,
                LocalDateTime.of(2026, 10, 17, 12, 0));
    }

    private GameResultParticipant participant(
            Long id,
            Team team,
            int arrestCount,
            int arrestedCount,
            ParticipantStatus status,
            Double distanceMeters,
            LocalDateTime leftAt
    ) {
        GameResultParticipant participant = GameResultParticipant.builder()
                .userId(id)
                .nickname("참가자" + id)
                .team(team)
                .status(status)
                .arrestCount(arrestCount)
                .arrestedCount(arrestedCount)
                .distanceMeters(distanceMeters)
                .leftAt(leftAt)
                .build();
        setId(participant, id);
        return participant;
    }
}
