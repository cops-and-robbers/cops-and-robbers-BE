package com.team.cops_and_robbers.game.game.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.game.game.application.GameService;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameInfoCommand;
import com.team.cops_and_robbers.game.game.application.dto.result.GameCreateResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameInfoResult;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameCreateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameCreateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameInfoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController implements GameControllerDocs {

    private final GameService gameService;

    /**
     * 1. 게임 방을 생성하고 초대 코드를 반환합니다.
     */
    @PostMapping
    public ResponseEntity<GameCreateResponse> createGame(
            @AuthUser LoginUser loginUser,
            @RequestBody @Valid GameCreateRequest request
    ) {
        GameCreateCommand command = GameCreateCommand.of(loginUser.userId(), request.area(), request.settings());
        GameCreateResult result = gameService.createGame(command);
        GameCreateResponse response = GameCreateResponse.from(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. 게임 기본 설정 정보를 조회합니다.
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameInfoResponse> getGameInfo(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId
    ) {
        GameInfoCommand command = GameInfoCommand.of(loginUser.userId(), gameId);
        GameInfoResult result = gameService.getGameInfo(command);
        GameInfoResponse response = GameInfoResponse.from(result);
        return ResponseEntity.ok(response);
    }

}
