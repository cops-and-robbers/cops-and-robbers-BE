package com.team.cops_and_robbers.history.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.history.application.GameResultService;
import com.team.cops_and_robbers.history.application.dto.command.GameResultCommand;
import com.team.cops_and_robbers.history.application.dto.result.GameResultResult;
import com.team.cops_and_robbers.history.presentation.dto.response.GameResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game-results")
@RequiredArgsConstructor
public class GameResultController implements GameResultControllerDocs {

    private final GameResultService gameResultService;

    @GetMapping("/{gameResultId}")
    public ResponseEntity<GameResultResponse> getGameResult(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameResultId
    ) {
        GameResultCommand command = GameResultCommand.of(loginUser.userId(), gameResultId);
        GameResultResult result = gameResultService.getGameResult(command);
        GameResultResponse response = GameResultResponse.from(result);
        return ResponseEntity.ok(response);
    }
}
