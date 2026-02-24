package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameHighlights;
import com.team.cops_and_robbers.history.domain.GameResult;
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
        // TODO: 통계 수집 - 추후 레디스 조회로 최적화
        int totalPolice = gameParticipantRepository.countByGameIdAndTeam(
                game.getId(), Team.POLICE
        );
        int totalRobber = gameParticipantRepository.countByGameIdAndTeam(
                game.getId(), Team.ROBBER
        );
        int capturedRobber = gameParticipantRepository.countByGameIdAndRobberStatus(
                game.getId(), ParticipantStatus.JAILED
        );

        GameArea gameArea = gameAreaRepository.getByGameId(game.getId());
        GameHighlights highlights = aggregateGameHighlights();
        GameResult result = GameResult.createSnapshot(
                game,
                winner,
                reason,
                totalPolice,
                totalRobber,
                capturedRobber,
                highlights,
                gameArea.getPlaygroundCenter(),
                gameArea.getPlaygroundRadiusInMeters()
        );

        return gameResultRepository.save(result);
    }


    // TODO: 게임 MVP 및 게임 요약 정보를 집계 - 추후 구체화
    private static GameHighlights aggregateGameHighlights() {
        GameHighlights.MvpRecord police =
                GameHighlights.MvpRecord.of(
                        1L,
                        "임시 경찰 MVP",
                        4
                );

        GameHighlights.MvpRecord robber =
                GameHighlights.MvpRecord.of(
                        2L,
                        "임시 도둑 MVP",
                        2
                );
        return GameHighlights.of(police, robber);
    }

}
