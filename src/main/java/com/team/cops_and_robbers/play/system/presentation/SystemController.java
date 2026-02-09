package com.team.cops_and_robbers.play.system.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.play.system.application.SystemService;
import com.team.cops_and_robbers.play.system.application.dto.ArrestCommand;
import com.team.cops_and_robbers.play.system.application.dto.EscapeCommand;
import com.team.cops_and_robbers.play.system.presentation.dto.ArrestRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/{gameId}/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    /**
     * 1. 경찰이 도둑을 체포합니다.
     */
    @PostMapping("/arrest")
    public ResponseEntity<Void> arrestRobber(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId,
            @RequestBody @Valid ArrestRequest request
    ) {
        ArrestCommand command = ArrestCommand.of(gameId, loginUser.userId(), request.robberParticipantId());
        systemService.arrestRobber(command);
        return ResponseEntity.ok().build();
    }

    /**
     * 2. 수감된 도둑이 탈옥합니다.
     */
    @PostMapping("/escape")
    public ResponseEntity<Void> escapeFromPrison(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId
    ) {
        EscapeCommand command = EscapeCommand.of(gameId, loginUser.userId());
        systemService.escapeFromPrison(command);
        return ResponseEntity.ok().build();
    }
}
