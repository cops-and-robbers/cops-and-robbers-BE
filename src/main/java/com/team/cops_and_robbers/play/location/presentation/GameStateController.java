package com.team.cops_and_robbers.play.location.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.play.location.application.GameStateService;
import com.team.cops_and_robbers.play.location.application.dto.command.GameStateCommand;
import com.team.cops_and_robbers.play.location.application.dto.result.GameStateResult;
import com.team.cops_and_robbers.play.location.presentation.dto.GameStateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameStateController implements GameStateControllerDocs {

    private final GameStateService gameStateService;

    /**
     * 1. 진행중인 게임의 상태를 응답합니다 (소켓 재연결 시 상태 회복을 위한 API)
     *   - 현재 잡히지 않은 도둑의 위치
     *   - 모든 참가자들의 state
     */
    @GetMapping("/{gameId}/state")
    public ResponseEntity<GameStateResponse> getGameState(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId
    ) {
        GameStateCommand command = GameStateCommand.of(gameId, loginUser.userId());
        GameStateResult result = gameStateService.getGameState(command);
        return ResponseEntity.ok(GameStateResponse.from(result));
    }
}
