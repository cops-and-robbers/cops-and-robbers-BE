package com.team.cops_and_robbers.play.location.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.location.application.dto.command.RobberLocationsCommand;
import com.team.cops_and_robbers.play.location.application.dto.result.RobberLocationResult;
import com.team.cops_and_robbers.play.location.presentation.dto.LocationUpdateRequest;
import com.team.cops_and_robbers.play.location.presentation.dto.RobberLocationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class LocationController implements LocationControllerDocs {

    private final RobberLocationService robberLocationService;

    @MessageMapping("/game/{gameId}/location")
    public void updateLocation(
            @DestinationVariable Long gameId,
            @Payload LocationUpdateRequest request,
            SimpMessageHeaderAccessor accessor
    ) {
        Long participantId = StompSessionHelper.getParticipantId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));

        robberLocationService.updateLocation(LocationUpdateCommand.of(gameId, participantId, request));
    }

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
