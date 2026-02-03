package com.team.cops_and_robbers.play.location.presentation;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.location.presentation.dto.LocationUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LocationController {

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
}
