package com.team.cops_and_robbers.play.location.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.location.application.dto.command.RobberLocationsCommand;
import com.team.cops_and_robbers.play.location.application.dto.result.RobberLocationResult;
import com.team.cops_and_robbers.play.location.presentation.dto.RobberLocationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class RobberLocationController implements RobberLocationControllerDocs {

    private final RobberLocationService robberLocationService;

    @Deprecated
    @GetMapping("/{gameId}/robbers/location")
    public ResponseEntity<List<RobberLocationResponse>> getRobberLocations(
            @AuthUser LoginUser loginUser,
            @PathVariable Long gameId
    ) {
        RobberLocationsCommand command = RobberLocationsCommand.of(gameId, loginUser.userId());
        List<RobberLocationResult> results = robberLocationService.getRobberLocations(command);
        List<RobberLocationResponse> response = results.stream()
                .map(RobberLocationResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
