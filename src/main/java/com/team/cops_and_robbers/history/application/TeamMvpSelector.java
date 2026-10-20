package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 팀별 MVP를 한 명씩 고릅니다. 중도 퇴장자는 후보에서 제외하며, 팀원이 전원 퇴장하면 뽑지 않습니다.
 * - 경찰: 체포 많은 순 → 이동 거리 긴 순 → 먼저 참가한 순
 * - 도둑: 잡힌 횟수 적은 순 → 종료 시점 감옥 밖 우선 → 이동 거리 긴 순 → 먼저 참가한 순
 * <p>
 * 거리는 앱이 계산해 보내는 값이라 서버가 검증할 수단이 없어, 체포 수가 같을 때만 쓰는 2차 키입니다.
 * 거리를 한 번도 받지 못한 참가자는 0m로 봅니다. 마지막 기준인 id는 명단에 올라간 순서, 곧 참가 순서입니다.
 */
@Component
public class TeamMvpSelector {

    private static final double NO_DISTANCE = 0.0;

    private static final Comparator<GameResultParticipant> POLICE_ORDER =
            Comparator.<GameResultParticipant>comparingInt(GameResultParticipant::getArrestCount).reversed()
                    .thenComparing(Comparator.<GameResultParticipant>comparingDouble(
                            TeamMvpSelector::distanceOf).reversed())
                    .thenComparing(GameResultParticipant::getId);

    private static final Comparator<GameResultParticipant> ROBBER_ORDER =
            Comparator.<GameResultParticipant>comparingInt(GameResultParticipant::getArrestedCount)
                    .thenComparing(TeamMvpSelector::isJailed)
                    .thenComparing(Comparator.<GameResultParticipant>comparingDouble(
                            TeamMvpSelector::distanceOf).reversed())
                    .thenComparing(GameResultParticipant::getId);

    public Optional<GameResultParticipant> selectPoliceMvp(List<GameResultParticipant> participants) {
        return candidatesOf(participants, Team.POLICE).stream().min(POLICE_ORDER);
    }

    public Optional<GameResultParticipant> selectRobberMvp(List<GameResultParticipant> participants) {
        return candidatesOf(participants, Team.ROBBER).stream().min(ROBBER_ORDER);
    }

    private static List<GameResultParticipant> candidatesOf(
            List<GameResultParticipant> participants,
            Team team
    ) {
        return participants.stream()
                .filter(participant -> participant.getTeam() == team && !participant.hasLeft())
                .toList();
    }

    private static double distanceOf(GameResultParticipant participant) {
        Double distance = participant.getDistanceMeters();
        return distance == null ? NO_DISTANCE : distance;
    }

    private static Boolean isJailed(GameResultParticipant participant) {
        return participant.getStatus() == ParticipantStatus.JAILED;
    }
}
