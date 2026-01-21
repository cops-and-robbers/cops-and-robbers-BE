package com.team.cops_and_robbers.game.participant.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.game.participant.application.GameParticipantService;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameJoinCommand;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameLeaveCommand;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameJoinResult;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameLeaveResult;
import com.team.cops_and_robbers.game.participant.presentation.dto.request.GameJoinRequest;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameJoinResponse;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameLeaveResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{gameId}/participants")
@RequiredArgsConstructor
public class GameParticipantController implements GameParticipantControllerDocs {

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
        GameJoinCommand command = GameJoinCommand.of(loginUser.userId(), gameId, request.inviteCode());
        GameJoinResult result = gameParticipantService.joinGame(command);
        GameJoinResponse response = GameJoinResponse.from(result);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 2. 게임 방에서 퇴장합니다.
     */
    @DeleteMapping
    public ResponseEntity<GameLeaveResponse> leaveGame(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId
    ) {
        GameLeaveCommand command = GameLeaveCommand.of(loginUser.userId(), gameId);
        GameLeaveResult result = gameParticipantService.leaveGame(command);
        GameLeaveResponse response = GameLeaveResponse.from(result);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
