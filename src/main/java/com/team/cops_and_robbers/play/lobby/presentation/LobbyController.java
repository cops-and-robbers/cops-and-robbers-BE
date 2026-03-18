package com.team.cops_and_robbers.play.lobby.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.play.lobby.application.LobbyService;
import com.team.cops_and_robbers.play.lobby.application.dto.command.GameStartCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.KickCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.LobbyInfoCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.ReadyUpdateCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.TeamChangeCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.result.LobbyInfoResult;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.ReadyUpdateRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.TeamChangeRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.response.LobbyInfoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{gameId}/lobby")
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

    /**
     * 4. 로비 정보를 조회합니다.
     */
    @GetMapping
    public ResponseEntity<LobbyInfoResponse> getLobbyInfo(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId
    ) {
        LobbyInfoCommand command = LobbyInfoCommand.of(loginUser.userId(), gameId);
        LobbyInfoResult result = lobbyService.getLobbyInfo(command);
        LobbyInfoResponse response = LobbyInfoResponse.from(result);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. 방장이 멤버를 퇴장시킵니다.
     */
    @DeleteMapping("/{participantId}")
    public ResponseEntity<Void> kickMember(
        @AuthUser LoginUser loginUser,
        @PathVariable Long gameId,
        @PathVariable Long participantId
    ){
        KickCommand command = KickCommand.of(loginUser.userId(), gameId, participantId);
        lobbyService.kickMember(command);
        return ResponseEntity.noContent().build();
    }
}
