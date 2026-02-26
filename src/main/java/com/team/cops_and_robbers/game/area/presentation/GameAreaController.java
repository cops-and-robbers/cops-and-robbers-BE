package com.team.cops_and_robbers.game.area.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.game.area.application.GameAreaService;
import com.team.cops_and_robbers.game.area.application.dto.command.GameAreaCommand;
import com.team.cops_and_robbers.game.area.application.dto.result.GameAreaResult;
import com.team.cops_and_robbers.game.area.presentation.dto.response.GameAreaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{gameId}/area")
@RequiredArgsConstructor
public class GameAreaController implements GameAreaControllerDocs {

    private final GameAreaService gameAreaService;

    @GetMapping
    public ResponseEntity<GameAreaResponse> getGameArea(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId
    ) {
        GameAreaCommand command = GameAreaCommand.of(loginUser.userId(), gameId);
        GameAreaResult result = gameAreaService.getGameArea(command);
        GameAreaResponse response = GameAreaResponse.from(result);
        return ResponseEntity.ok(response);
    }
}
