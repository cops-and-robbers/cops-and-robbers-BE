package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
