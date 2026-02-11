package com.team.cops_and_robbers.play.system.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.play.system.application.SystemService;
import com.team.cops_and_robbers.play.system.application.dto.command.ArrestCommand;
import com.team.cops_and_robbers.play.system.application.dto.command.EscapeCommand;
import com.team.cops_and_robbers.play.system.application.dto.result.ArrestResult;
import com.team.cops_and_robbers.play.system.presentation.dto.request.ArrestRequest;
import com.team.cops_and_robbers.play.system.presentation.dto.response.ArrestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{gameId}/system")
@RequiredArgsConstructor
public class SystemController implements SystemControllerDocs {

    private final SystemService systemService;

    /**
     * 1. 경찰이 도둑을 체포합니다.
     */
    @PostMapping("/arrest")
    public ResponseEntity<ArrestResponse> arrestRobber(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId,
            @RequestBody @Valid ArrestRequest request
    ) {
        ArrestCommand command = ArrestCommand.of(gameId, loginUser.userId(), request.robberParticipantId());
        ArrestResult result = systemService.arrestRobber(command);
        return ResponseEntity.ok(ArrestResponse.from(result));
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
        return ResponseEntity.noContent().build();
    }
}
