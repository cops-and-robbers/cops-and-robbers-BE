package com.team.cops_and_robbers.play.chat.presentation;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.play.chat.application.ChatPublisher;
import com.team.cops_and_robbers.play.chat.presentation.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatPublisher chatPublisher;

    @MessageMapping("/game/{gameId}/chat")
    public void sendMessage(@DestinationVariable Long gameId,
                            @Payload ChatRequest request,
                            SimpMessageHeaderAccessor accessor
    ) {
        Long participantId = StompSessionHelper.getParticipantId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));
        Long userId = StompSessionHelper.getUserId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));
        chatPublisher.processAndRouteMessage(request.toCommand(gameId, userId, participantId));
    }
}
