package com.team.cops_and_robbers.play.radio.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.play.radio.application.RadioService;
import com.team.cops_and_robbers.play.radio.application.dto.command.RadioTokenCommand;
import com.team.cops_and_robbers.play.radio.application.dto.result.RadioTokenResult;
import com.team.cops_and_robbers.play.radio.presentation.dto.response.RadioTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{gameId}/radio")
@RequiredArgsConstructor
public class RadioController implements RadioControllerDocs {

    private final RadioService radioService;

    /**
     * 1. 경찰이 무전 접속 토큰을 발급받습니다.
     */
    @PostMapping("/token")
    public ResponseEntity<RadioTokenResponse> issueToken(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId
    ) {
        RadioTokenCommand command = RadioTokenCommand.of(gameId, loginUser.userId());
        RadioTokenResult result = radioService.issueToken(command);
        return ResponseEntity.ok(RadioTokenResponse.from(result));
    }
}
