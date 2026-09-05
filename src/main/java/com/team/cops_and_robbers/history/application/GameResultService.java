package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.application.dto.command.GameResultCommand;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameResultService {

    private final GameResultRepository gameResultRepository;
    private final GameResultParticipantRepository gameResultParticipantRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;

    /**
     * 게임이 시작되면 확인자료를 열고 그 시점의 참가자 명단을 남깁니다.
     * <p>
     * 좌표 제공이 시작되는 시점에 명단을 박아 두어야, 게임 중 퇴장해 participants 행이 지워진
     * 사람도 위치정보법 제16조 제2항의 "제공받은 자"로 남습니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public GameResult openGameResult(Game game) {
        GameArea gameArea = gameAreaRepository.getByGameId(game.getId());
        GameResult openedResult = gameResultRepository.save(GameResult.openSnapshot(game, gameArea));
        saveParticipantSnapshots(openedResult, game.getId());

        return openedResult;
    }

    /**
     * 게임이 시작된 뒤 들어온 참가자를 확인자료 명단에 추가합니다.
     * <p>
     * 일반 게임은 WAITING 상태에서만 입장할 수 있어 시작 시점 명단으로 충분하지만,
     * 이벤트 게임은 반대로 IN_PROGRESS 상태에서만 입장할 수 있어 여기서 따로 담아야 합니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordParticipantJoined(Long gameId, GameParticipant participant) {
        findInProgressResult(gameId).ifPresent(result -> gameResultParticipantRepository.save(
                GameResultParticipant.createSnapshot(result, participant)
        ));
    }

    /**
     * 게임 중 퇴장을 확인자료에 남깁니다. 명단에서 지우지 않고 퇴장 시각만 찍습니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordParticipantLeft(Long gameId, Long userId) {
        findInProgressResult(gameId)
                .flatMap(result -> gameResultParticipantRepository
                        .findByGameResultIdAndUserIdAndLeftAtIsNull(result.getId(), userId))
                .ifPresent(GameResultParticipant::markLeft);
    }

    /**
     * 게임 종료 시점의 통계를 수집해 열어 둔 GameResult 를 완성합니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public GameResult recordGameResult(Game game, Team winner, GameEndReason reason) {
        GameResult result = findInProgressResult(game.getId())
                .orElseGet(() -> openGameResult(game));

        updateFinalParticipantStatuses(result, game.getId());

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

    private void saveParticipantSnapshots(GameResult gameResult, Long gameId) {
        List<GameResultParticipant> snapshots =
                gameParticipantRepository.findAllByGameIdWithUser(gameId)
                        .stream()
                        .map(gp -> GameResultParticipant.createSnapshot(gameResult, gp))
                        .toList();
        gameResultParticipantRepository.saveAll(snapshots);
    }

    private void updateFinalParticipantStatuses(GameResult gameResult, Long gameId) {
        Map<Long, ParticipantStatus> statusByUserId =
                gameParticipantRepository.findAllByGameIdWithUser(gameId)
                        .stream()
                        .collect(Collectors.toMap(
                                participant -> participant.getUser().getId(),
                                GameParticipant::getStatus
                        ));

        gameResultParticipantRepository.findByGameResultId(gameResult.getId())
                .forEach(snapshot -> snapshot.updateFinalStatus(statusByUserId.get(snapshot.getUserId())));
    }

    @Transactional(readOnly = true)
    public GameResultResult getGameResult(GameResultCommand command) {
        GameResult gameResult = gameResultRepository.findById(command.gameResultId())
                .filter(GameResult::isCompleted)
                .orElseThrow(() -> new ApplicationException(GameResultException.GAME_RESULT_NOT_FOUND));
        gameParticipantRepository.getByGameIdAndUserId(gameResult.getGameId(), command.userId());
        return GameResultResult.from(gameResult);
    }

}
