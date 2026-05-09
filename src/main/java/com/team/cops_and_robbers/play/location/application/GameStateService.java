package com.team.cops_and_robbers.play.location.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.location.application.dto.command.GameStateCommand;
import com.team.cops_and_robbers.play.location.application.dto.result.GameStateResult;
import com.team.cops_and_robbers.play.location.repository.RobberLocationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameStateService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final RobberLocationSnapshotRepository robberLocationSnapshotRepository;

    /**
     * 1. 진행중인 게임의 상태를 응답합니다
     *   - 현재 잡히지 않은 도둑의 위치
     *   - 모든 참가자들의 state
     */
    public GameStateResult getGameState(GameStateCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        validateGameInProgress(game);

        List<GameParticipant> allParticipants = gameParticipantRepository.findAllByGameIdWithUser(command.gameId());
        validateIsParticipant(allParticipants, command.userId());

        return new GameStateResult(
                buildRobberLocations(command.gameId(), allParticipants),
                buildParticipantInfos(allParticipants)
        );
    }

    private void validateGameInProgress(Game game) {
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
    }

    private void validateIsParticipant(List<GameParticipant> participants, Long userId) {
        boolean isParticipant = participants.stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
        if (!isParticipant) {
            throw new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND);
        }
    }

    private List<GameStateResult.ParticipantInfo> buildParticipantInfos(List<GameParticipant> participants) {
        return participants.stream()
                .map(GameStateResult.ParticipantInfo::from)
                .toList();
    }

    /**
     * 현재 ALIVE 상태의 도둑들의 위치를 최근 위치 공개 시점의 스냅샷에서 조회합니다.
     */
    private List<GameStateResult.RobberLocationInfo> buildRobberLocations(
            Long gameId, List<GameParticipant> allParticipants) {
        if (isPoliceWaitingPhase(allParticipants)) {
            return List.of();
        }
        Set<Long> aliveRobberIds = extractAliveRobberIds(allParticipants);
        return robberLocationSnapshotRepository.findAllByGameId(gameId).stream()
                .filter(location -> aliveRobberIds.contains(location.participantId()))
                .map(GameStateResult.RobberLocationInfo::from)
                .toList();
    }

    /**
     * Waiting 중인 경찰있는지 검증합니다.
     */
    private boolean isPoliceWaitingPhase(List<GameParticipant> participants) {
        return participants.stream()
                .anyMatch(GameParticipant::isPoliceWaiting);
    }

    /**
     * 잡히지 않은 도둑들의 participantId를 반환합니다.
     */
    private Set<Long> extractAliveRobberIds(List<GameParticipant> participants) {
        return participants.stream()
                .filter(p -> p.isRobber() && !p.isJailed())
                .map(GameParticipant::getId)
                .collect(Collectors.toSet());
    }
}
