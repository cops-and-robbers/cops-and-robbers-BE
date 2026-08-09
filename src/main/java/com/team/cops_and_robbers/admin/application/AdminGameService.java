package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.command.game.AdminGameListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameDetailResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGamePageResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameSummaryResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminParticipantResult;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantCountProjection;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.repository.GameResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public Map<AdminGameResult, AdminGameDetailResult> getResultsByGame(List<AdminGameResult> games) {
        List<Long> gameIds = games.stream().map(AdminGameResult::id).toList();
        Map<Long, AdminGameDetailResult> resultByGameId = gameResultRepository.findByGameIdIn(gameIds)
                .stream()
                .collect(Collectors.toMap(
                        GameResult::getGameId,
                        AdminGameDetailResult::from
                ));
        return games.stream()
                .filter(game -> resultByGameId.containsKey(game.id()))
                .collect(Collectors.toMap(
                        game -> game,
                        game -> resultByGameId.get(game.id())
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
}
