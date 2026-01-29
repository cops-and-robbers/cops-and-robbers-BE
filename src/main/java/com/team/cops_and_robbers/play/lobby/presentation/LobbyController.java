package com.team.cops_and_robbers.play.lobby.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.play.lobby.application.LobbyService;
import com.team.cops_and_robbers.play.lobby.application.dto.command.TeamChangeCommand;
import com.team.cops_and_robbers.play.lobby.presentation.dto.TeamChangeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/games/{gameId}")
@RequiredArgsConstructor
public class LobbyController implements LobbyControllerDocs {

    private final LobbyService lobbyService;

    /**
     * 1. 대기실 내 유저의 팀을 변경합니다. (경찰 <-> 도둑)
     */
    @PatchMapping("/team")
    public ResponseEntity<Void> changeTeam(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId,
            @RequestBody @Valid TeamChangeRequest request
    ) {
        TeamChangeCommand command = TeamChangeCommand.of(loginUser.userId(), gameId, request.targetTeam());
        lobbyService.changeTeam(command);
        return ResponseEntity.noContent().build();
    }
}
