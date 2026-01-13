package com.team.cops_and_robbers.game.game.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.game.game.application.GameService;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameCreateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * 1. 게임 방을 생성하고 초대 코드를 반환합니다.
     */
    @PostMapping
    public ResponseEntity<GameCreateResponse> createGame(
            @AuthUser LoginUser loginUser,
            @RequestBody @Valid GameCreateRequest request
    ) {
        Game game = gameService.createGame(loginUser.userId(), request.toCommand());
        GameCreateResponse response = GameCreateResponse.from(game);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
