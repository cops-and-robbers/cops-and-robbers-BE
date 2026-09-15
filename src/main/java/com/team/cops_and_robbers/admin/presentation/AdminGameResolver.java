package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.admin.application.AdminGameHistoryService;
import com.team.cops_and_robbers.admin.application.AdminGameService;
import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.game.AdminGameHistoryListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.game.AdminGameListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameDetailResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameHistoryPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameHistoryParticipantResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameHistoryResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGamePageResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminParticipantResult;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Validated
@Controller
@RequiredArgsConstructor
public class AdminGameResolver {

    private final AdminGameService adminGameService;
    private final AdminGameHistoryService adminGameHistoryService;

    @QueryMapping
    public AdminGamePageResult adminGames(
            @Argument @Min(0) int page,
            @Argument @Min(1) @Max(100) int size,
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
        return adminGameService.getLatestResultByGame(games);
    }

    @BatchMapping(typeName = "AdminGame", field = "results")
    public Map<AdminGameResult, List<AdminGameDetailResult>> results(
            List<AdminGameResult> games) {
        return adminGameService.getResultsByGame(games);
    }

    @BatchMapping(typeName = "AdminGame", field = "area")
    public Map<AdminGameResult, AdminGameAreaResult> area(
            List<AdminGameResult> games) {
        return adminGameService.getAreasByGame(games);
    }

    @QueryMapping
    public AdminGameHistoryPageResult adminGameHistories(
            @Argument @Min(0) int page,
            @Argument @Min(1) @Max(100) int size,
            @Argument GameEndReason endReason,
            @Argument SortDirection sortDirection
    ) {
        AdminGameHistoryListCommand command = new AdminGameHistoryListCommand(
                page, size, endReason, sortDirection != null ? sortDirection : SortDirection.DESC);
        return adminGameHistoryService.getGameHistoryList(command);
    }

    @QueryMapping
    public AdminGameHistoryResult adminGameHistory(@Argument Long id) {
        return adminGameHistoryService.getGameHistory(id);
    }

    @BatchMapping(typeName = "AdminGameHistory", field = "participants")
    public Map<AdminGameHistoryResult, List<AdminGameHistoryParticipantResult>> historyParticipants(
            List<AdminGameHistoryResult> histories) {
        return adminGameHistoryService.getHistoryParticipantsByGame(histories);
    }
}
