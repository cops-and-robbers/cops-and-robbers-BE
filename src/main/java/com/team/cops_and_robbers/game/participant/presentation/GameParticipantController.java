package com.team.cops_and_robbers.game.participant.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.game.participant.application.GameParticipantService;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameJoinResult;
import com.team.cops_and_robbers.game.participant.presentation.dto.request.GameJoinRequest;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameJoinResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{gameId}/participants")
@RequiredArgsConstructor
public class GameParticipantController {

    private final GameParticipantService gameParticipantService;

    /**
     * 1. 게임 방에 참여합니다.
     */
    @PostMapping
    public ResponseEntity<GameJoinResponse> joinGame(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId,
            @RequestBody @Valid GameJoinRequest request
    ) {
        GameJoinResult result = gameParticipantService.joinGame(loginUser.userId(), gameId, request.toCommand());
        GameJoinResponse response = GameJoinResponse.from(result);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
