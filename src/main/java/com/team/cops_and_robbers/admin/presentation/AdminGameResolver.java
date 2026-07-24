package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.admin.application.AdminGameService;
import com.team.cops_and_robbers.admin.application.dto.command.AdminGameListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.AdminGameAreaResult;
import com.team.cops_and_robbers.admin.application.dto.result.AdminGamePageResult;
import com.team.cops_and_robbers.admin.application.dto.result.AdminGameResult;
import com.team.cops_and_robbers.admin.application.dto.result.AdminGameDetailResult;
import com.team.cops_and_robbers.admin.application.dto.result.AdminParticipantResult;
import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminGameResolver {

    private final AdminGameService adminGameService;

    @QueryMapping
    public AdminGamePageResult adminGames(
            @Argument int page,
            @Argument int size,
            @Argument GameStatus status,
            @Argument SortDirection sortDirection
    ) {
        AdminGameListCommand command = new AdminGameListCommand(
                page, size, status, sortDirection != null ? sortDirection : SortDirection.DESC);
        return adminGameService.getGameList(command);
    }

    @QueryMapping
    public AdminGameResult adminGame(@Argument Long id) {
        return adminGameService.getGame(id);
    }

    @BatchMapping(typeName = "AdminGame", field = "participants")
    public Map<AdminGameResult, List<AdminParticipantResult>> participants(
            List<AdminGameResult> games) {
        return adminGameService.getParticipantsByGame(games);
    }

    @BatchMapping(typeName = "AdminGame", field = "result")
    public Map<AdminGameResult, AdminGameDetailResult> result(
            List<AdminGameResult> games) {
        return adminGameService.getResultsByGame(games);
    }

    @BatchMapping(typeName = "AdminGame", field = "area")
    public Map<AdminGameResult, AdminGameAreaResult> area(
            List<AdminGameResult> games) {
        return adminGameService.getAreasByGame(games);
    }
}
