package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.command.game.AdminGameListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.game.AdminUpdateEventGameCommand;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameDetailResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGamePageResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameSummaryResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminParticipantResult;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantCountProjection;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.repository.GameResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminGameService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameResultRepository gameResultRepository;

    public AdminGamePageResult getGameList(AdminGameListCommand command) {
        Page<Game> gamePage = gameRepository.findAllForAdmin(command.status(), command.toPageable());
        List<Long> gameIds = gamePage.getContent().stream().map(Game::getId).toList();
        Map<Long, Integer> countByGameId = gameParticipantRepository.countByGameIdIn(gameIds)
                .stream()
                .collect(Collectors.toMap(
                        GameParticipantCountProjection::gameId,
                        p -> p.count().intValue()
                ));
        Page<AdminGameSummaryResult> summaryPage = gamePage.map(game ->
                AdminGameSummaryResult.from(game, countByGameId.getOrDefault(game.getId(), 0)));
        return AdminGamePageResult.from(summaryPage);
    }

    public AdminGameResult getGame(Long gameId) {
        Game game = gameRepository.getByGameId(gameId);
        return AdminGameResult.from(game);
    }

    /**
     * 방을 이벤트 게임으로 전환합니다.
     * 진행 중에 켜면 시작 시점에 복사해 둔 값이 false 로 남으므로 열려 있는 결과도 함께 바꿉니다.
     */
    @Transactional
    public AdminGameResult updateEventGame(AdminUpdateEventGameCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        validateEventGameChangeable(game, command.isEventGame());

        game.updateEventGame(command.isEventGame());
        gameResultRepository.findByGameIdAndEndReasonIsNull(game.getId())
                .ifPresent(openedResult -> openedResult.updateEventGame(command.isEventGame()));

        log.info("[EventGame] 이벤트 게임 전환 | gameId={}, isEventGame={}, status={}",
                game.getId(), command.isEventGame(), game.getStatus());

        return AdminGameResult.from(game);
    }

    public Map<AdminGameResult, List<AdminParticipantResult>> getParticipantsByGame(
            List<AdminGameResult> games) {
        List<Long> gameIds = games.stream().map(AdminGameResult::id).toList();
        Map<Long, List<AdminParticipantResult>> participantsByGameId =
                gameParticipantRepository.findByGameIdInWithUser(gameIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                gp -> gp.getGame().getId(),
                                Collectors.mapping(AdminParticipantResult::from, Collectors.toList())
                        ));
        return games.stream().collect(Collectors.toMap(
                game -> game,
                game -> participantsByGameId.getOrDefault(game.id(), List.of())
        ));
    }

    public Map<AdminGameResult, List<AdminGameDetailResult>> getResultsByGame(List<AdminGameResult> games) {
        List<Long> gameIds = games.stream().map(AdminGameResult::id).toList();
        Map<Long, List<AdminGameDetailResult>> resultsByGameId =
                gameResultRepository.findCompletedByGameIdIn(gameIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                GameResult::getGameId,
                                Collectors.mapping(AdminGameDetailResult::from, Collectors.toList())
                        ));
        return games.stream().collect(Collectors.toMap(
                game -> game,
                game -> resultsByGameId.getOrDefault(game.id(), List.of())
        ));
    }

    public Map<AdminGameResult, AdminGameDetailResult> getLatestResultByGame(List<AdminGameResult> games) {
        List<Long> gameIds = games.stream().map(AdminGameResult::id).toList();
        Map<Long, AdminGameDetailResult> latestResultByGameId =
                gameResultRepository.findCompletedByGameIdIn(gameIds)
                        .stream()
                        .collect(Collectors.toMap(
                                GameResult::getGameId,
                                AdminGameDetailResult::from,
                                (previous, latest) -> latest
                        ));
        return games.stream()
                .filter(game -> latestResultByGameId.containsKey(game.id()))
                .collect(Collectors.toMap(
                        game -> game,
                        game -> latestResultByGameId.get(game.id())
                ));
    }

    public Map<AdminGameResult, AdminGameAreaResult> getAreasByGame(List<AdminGameResult> games) {
        List<Long> gameIds = games.stream().map(AdminGameResult::id).toList();
        Map<Long, AdminGameAreaResult> areaByGameId = gameAreaRepository.findByGameIdIn(gameIds)
                .stream()
                .collect(Collectors.toMap(
                        ga -> ga.getGame().getId(),
                        AdminGameAreaResult::from
                ));
        return games.stream()
                .filter(game -> areaByGameId.containsKey(game.id()))
                .collect(Collectors.toMap(
                        game -> game,
                        game -> areaByGameId.get(game.id())
                ));
    }

    /**
     * 끄기를 대기 중으로만 제한하는 이유는, 진행 중에 끄면 이벤트로 들어온 경찰이 일반 규칙 게임에 남고
     * 마지막 경찰이 나갈 때 기권패가 다시 적용되기 때문입니다.
     */
    private void validateEventGameChangeable(Game game, boolean isEventGame) {
        if (!isEventGame && !game.isWaiting()) {
            throw new ApplicationException(GameException.GAME_NOT_WAITING);
        }
        if (!game.isWaiting() && !game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_ACTIVE);
        }
    }
}
