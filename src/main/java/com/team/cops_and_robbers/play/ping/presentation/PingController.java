package com.team.cops_and_robbers.play.ping.presentation;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.play.ping.application.PingPublisher;
import com.team.cops_and_robbers.play.ping.presentation.dto.PingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PingController {

    private final PingPublisher pingPublisher;

    @MessageMapping("/game/{gameId}/ping")
    public void sendPing(@DestinationVariable Long gameId,
                         @Payload PingRequest request,
                         SimpMessageHeaderAccessor accessor) {
        Long participantId = StompSessionHelper.getParticipantId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));

        pingPublisher.route(request.toCommand(gameId, participantId));
    }
}
