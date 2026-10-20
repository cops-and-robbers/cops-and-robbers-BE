package com.team.cops_and_robbers.play.distance.presentation;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.play.distance.application.ParticipantDistanceService;
import com.team.cops_and_robbers.play.distance.application.dto.command.DistanceUpdateCommand;
import com.team.cops_and_robbers.play.distance.presentation.dto.DistanceUpdateRequest;
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
public class ParticipantDistanceController {

    private final ParticipantDistanceService participantDistanceService;

    @MessageMapping("/game/{gameId}/distance")
    public void updateDistance(
            @DestinationVariable Long gameId,
            @Payload DistanceUpdateRequest request,
            SimpMessageHeaderAccessor accessor
    ) {
        Long participantId = StompSessionHelper.getParticipantId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));

        participantDistanceService.updateDistance(DistanceUpdateCommand.of(gameId, participantId, request));
    }
}
