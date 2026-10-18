package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.application.dto.ParticipantDistance;
import com.team.cops_and_robbers.history.application.dto.command.GameResultCommand;
import com.team.cops_and_robbers.history.application.dto.result.GameResultParticipantResult;
import com.team.cops_and_robbers.history.application.dto.result.GameResultResult;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import com.team.cops_and_robbers.history.exception.GameResultException;
import com.team.cops_and_robbers.history.repository.GameResultParticipantRepository;
import com.team.cops_and_robbers.history.repository.GameResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameResultService {

    private final Clock clock;
    private final GameResultRepository gameResultRepository;
    private final GameResultParticipantRepository gameResultParticipantRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final TeamMvpSelector teamMvpSelector;

    /**
     * 게임이 시작되면 GameResult 를 열고 그 시점의 참가자 명단을 남깁니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public GameResult openGameResult(Game game) {
        GameArea gameArea = gameAreaRepository.getByGameId(game.getId());
        List<GameParticipant> participants = gameParticipantRepository.findAllByGameIdWithUser(game.getId());
        GameResult openedResult = gameResultRepository.save(
                GameResult.openSnapshot(game, gameArea, findHostUserId(participants))
        );
        saveParticipantSnapshots(openedResult, participants);

        return openedResult;
    }

    /**
     * 게임이 시작된 뒤 들어온 참가자를 명단에 추가합니다. (이벤트 게임은 IN_PROGRESS 에서 입장)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordParticipantJoined(Long gameId, GameParticipant participant) {
        findInProgressResult(gameId).ifPresent(result -> gameResultParticipantRepository.save(
                GameResultParticipant.createSnapshot(result, participant)
        ));
    }

    /**
     * 게임 중 퇴장을 기록합니다. 명단에서 지우지 않고 퇴장 시각만 찍습니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordParticipantLeft(Long gameId, Long userId) {
        findInProgressResult(gameId)
                .flatMap(result -> gameResultParticipantRepository
                        .findByGameResultIdAndUserIdAndLeftAtIsNull(result.getId(), userId))
                .ifPresent(snapshot -> snapshot.markLeft(LocalDateTime.now(clock)));
    }

    /**
     * 체포한 경찰의 체포수를 1 올립니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordArrest(Long gameId, Long policeUserId) {
        findInProgressResult(gameId)
                .flatMap(result -> gameResultParticipantRepository
                        .findByGameResultIdAndUserIdAndLeftAtIsNull(result.getId(), policeUserId))
                .ifPresent(GameResultParticipant::incrementArrestCount);
    }

    /**
     * 잡힌 도둑의 잡힌 횟수를 1 올립니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordArrested(Long gameId, Long robberUserId) {
        findInProgressResult(gameId)
                .flatMap(result -> gameResultParticipantRepository
                        .findByGameResultIdAndUserIdAndLeftAtIsNull(result.getId(), robberUserId))
                .ifPresent(GameResultParticipant::incrementArrestedCount);
    }

    /**
     * 게임 종료 시점의 통계를 수집해 열어 둔 GameResult 를 완성합니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public GameResult recordGameResult(
            Game game,
            Team winner,
            GameEndReason reason,
            List<ParticipantDistance> distances
    ) {
        GameResult result = findInProgressResult(game.getId())
                .orElseGet(() -> openGameResult(game));

        List<GameParticipant> participants = gameParticipantRepository.findAllByGameIdWithUser(game.getId());
        List<GameResultParticipant> snapshots = gameResultParticipantRepository.findByGameResultId(result.getId());

        updateFinalParticipantStatuses(snapshots, participants);
        applyDistances(snapshots, participants, distances);
        markTeamMvps(snapshots, result);

        int totalPolice = gameParticipantRepository.countByGameIdAndTeam(
                game.getId(), Team.POLICE
        );
        int totalRobber = gameParticipantRepository.countByGameIdAndTeam(
                game.getId(), Team.ROBBER
        );
        int jailedCountAtEnd = gameParticipantRepository.countByGameIdAndRobberStatus(
                game.getId(), ParticipantStatus.JAILED
        );

        result.complete(
                LocalDateTime.now(clock),
                winner,
                reason,
                totalPolice,
                totalRobber,
                jailedCountAtEnd,
                game.getTotalArrestCount()
        );

        return gameResultRepository.save(result);
    }

    private Optional<GameResult> findInProgressResult(Long gameId) {
        return gameResultRepository.findByGameIdAndEndReasonIsNull(gameId);
    }

    private Long findHostUserId(List<GameParticipant> participants) {
        return participants.stream()
                .filter(GameParticipant::isHost)
                .findFirst()
                .map(participant -> participant.getUser().getId())
                .orElse(null);
    }

    private void saveParticipantSnapshots(GameResult gameResult, List<GameParticipant> participants) {
        List<GameResultParticipant> snapshots = participants.stream()
                .map(gp -> GameResultParticipant.createSnapshot(gameResult, gp))
                .toList();
        gameResultParticipantRepository.saveAll(snapshots);
    }

    private void updateFinalParticipantStatuses(
            List<GameResultParticipant> snapshots,
            List<GameParticipant> participants
    ) {
        Map<Long, ParticipantStatus> statusByUserId = participants.stream()
                .collect(Collectors.toMap(
                        participant -> participant.getUser().getId(),
                        GameParticipant::getStatus
                ));

        snapshots.forEach(snapshot -> snapshot.updateFinalStatus(statusByUserId.get(snapshot.getUserId())));
    }

    private void applyDistances(
            List<GameResultParticipant> snapshots,
            List<GameParticipant> participants,
            List<ParticipantDistance> distances
    ) {
        if (distances.isEmpty()) {
            return;
        }

        Map<Long, Long> userIdByParticipantId = participants.stream()
                .collect(Collectors.toMap(GameParticipant::getId, participant -> participant.getUser().getId()));
        Map<Long, GameResultParticipant> snapshotByUserId = snapshots.stream()
                .filter(snapshot -> !snapshot.hasLeft())
                .collect(Collectors.toMap(GameResultParticipant::getUserId, snapshot -> snapshot));

        distances.forEach(distance -> {
            GameResultParticipant snapshot = snapshotByUserId.get(
                    userIdByParticipantId.get(distance.participantId())
            );
            if (snapshot != null) {
                snapshot.updateDistanceMeters(distance.distanceMeters());
            }
        });
    }

    private void markTeamMvps(List<GameResultParticipant> snapshots, GameResult result) {
        if (Boolean.TRUE.equals(result.getEventGame())) {
            return;
        }
        teamMvpSelector.selectPoliceMvp(snapshots).ifPresent(GameResultParticipant::markMvp);
        teamMvpSelector.selectRobberMvp(snapshots).ifPresent(GameResultParticipant::markMvp);
    }

    @Transactional(readOnly = true)
    public GameResultResult getGameResult(GameResultCommand command) {
        GameResult gameResult = gameResultRepository.findById(command.gameResultId())
                .filter(GameResult::isCompleted)
                .orElseThrow(() -> new ApplicationException(GameResultException.GAME_RESULT_NOT_FOUND));
        gameParticipantRepository.getByGameIdAndUserId(gameResult.getGameId(), command.userId());
        return GameResultResult.from(
                gameResult,
                gameResultParticipantRepository.findByGameResultIdAndIsMvpTrue(gameResult.getId())
        );
    }

    /**
     * 요청한 사용자 자신의 개인 기록을 조회합니다.
     * 명단에 없으면 그 게임 참가자가 아니므로 조회할 수 없습니다.
     */
    public GameResultParticipantResult getMyGameRecord(GameResultCommand command) {
        gameResultRepository.findById(command.gameResultId())
                .filter(GameResult::isCompleted)
                .orElseThrow(() -> new ApplicationException(GameResultException.GAME_RESULT_NOT_FOUND));

        return gameResultParticipantRepository
                .findFirstByGameResultIdAndUserIdOrderByIdDesc(command.gameResultId(), command.userId())
                .map(GameResultParticipantResult::from)
                .orElseThrow(() -> new ApplicationException(GameResultException.GAME_RESULT_NOT_FOUND));
    }

}
