package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.application.dto.result.GameResultResult;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.exception.GameResultException;
import com.team.cops_and_robbers.history.repository.GameResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameResultService {

    private final GameResultRepository gameResultRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;

    /**
     * 게임 종료 시점의 통계를 수집하고 GameResult 를 저장합니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public GameResult recordGameResult(Game game, Team winner, GameEndReason reason) {
        int totalPolice = gameParticipantRepository.countByGameIdAndTeam(
                game.getId(), Team.POLICE
        );
        int totalRobber = gameParticipantRepository.countByGameIdAndTeam(
                game.getId(), Team.ROBBER
        );
        int jailedAtEnd = gameParticipantRepository.countByGameIdAndRobberStatus(
                game.getId(), ParticipantStatus.JAILED
        );

        GameArea gameArea = gameAreaRepository.getByGameId(game.getId());
        GameResult result = GameResult.createSnapshot(
                game,
                winner,
                reason,
                totalPolice,
                totalRobber,
                jailedAtEnd,
                game.getArrestCount(),
                gameArea.getPlaygroundCenter(),
                gameArea.getPlaygroundRadiusInMeters()
        );

        return gameResultRepository.save(result);
    }

    @Transactional(readOnly = true)
    public GameResultResult getGameResult(Long gameResultId, Long userId) {
        GameResult gameResult = gameResultRepository.findById(gameResultId)
                .orElseThrow(() -> new ApplicationException(GameResultException.GAME_RESULT_NOT_FOUND));
        gameParticipantRepository.getByGameIdAndUserId(gameResult.getGameId(), userId);
        return GameResultResult.from(gameResult);
    }

}
