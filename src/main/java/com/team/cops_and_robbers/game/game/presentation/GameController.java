package com.team.cops_and_robbers.game.game.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.game.game.application.GameService;
import com.team.cops_and_robbers.game.game.application.dto.command.GameAreaUpdateCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameInfoCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameSettingsUpdateCommand;
import com.team.cops_and_robbers.game.game.application.dto.result.GameAreaUpdateResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameCreateResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameInfoResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameSettingsUpdateResult;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameAreaRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameCreateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameAreaUpdateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameCreateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameInfoResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameSettingsUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    /**
     * 3. 대기실에서 게임 영역을 수정하고 전체 영역 정보를 반환합니다. (방장 권한)
     */
    @PutMapping("/{gameId}/area")
    public ResponseEntity<GameAreaUpdateResponse> updateGameArea(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId,
            @RequestBody @Valid GameAreaRequest request
    ) {
        GameAreaUpdateCommand command = GameAreaUpdateCommand.of(gameId, loginUser.userId(), request);
        GameAreaUpdateResult result = gameService.updateGameArea(command);
        return ResponseEntity.ok(GameAreaUpdateResponse.from(result));
    }

    /**
     * 4. 대기실에서 게임 설정을 수정하고 전체 설정 정보를 반환합니다. (방장 권한)
     */
    @PutMapping("/{gameId}/settings")
    public ResponseEntity<GameSettingsUpdateResponse> updateGameSettings(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId,
            @RequestBody @Valid GameSettingsRequest request
    ) {
        GameSettingsUpdateCommand command = GameSettingsUpdateCommand.of(gameId, loginUser.userId(), request);
        GameSettingsUpdateResult result = gameService.updateGameSettings(command);
        return ResponseEntity.ok(GameSettingsUpdateResponse.from(result));
    }
}

