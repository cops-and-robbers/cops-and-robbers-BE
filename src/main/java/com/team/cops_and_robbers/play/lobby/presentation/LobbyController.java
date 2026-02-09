package com.team.cops_and_robbers.play.lobby.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.play.lobby.application.LobbyService;
import com.team.cops_and_robbers.play.lobby.application.dto.command.GameStartCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.ReadyUpdateCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.TeamChangeCommand;
import com.team.cops_and_robbers.play.lobby.presentation.dto.ReadyUpdateRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.TeamChangeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 2. 대기실 내 유저의 준비 상태를 변경합니다.
     */
    @PatchMapping("/ready")
    public ResponseEntity<Void> updateReady(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId,
            @RequestBody @Valid ReadyUpdateRequest request
    ) {
        ReadyUpdateCommand command = ReadyUpdateCommand.of(loginUser.userId(), gameId, request.isReady());
        lobbyService.updateReady(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * 3. 게임을 시작합니다.
     */
    @PostMapping("/start")
    public ResponseEntity<Void> startGame(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId
    ) {
        GameStartCommand command = GameStartCommand.of(loginUser.userId(), gameId);
        lobbyService.startGame(command);
        return ResponseEntity.noContent().build();
    }
}
