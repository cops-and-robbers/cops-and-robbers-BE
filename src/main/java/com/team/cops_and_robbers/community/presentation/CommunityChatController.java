package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.community.application.CommunityChatService;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class CommunityChatController {

    private final CommunityChatService communityChatService;

    @MessageMapping("/community/{postId}/chat")
    public void sendMessage(
            @DestinationVariable Long postId,
            @Payload CommunityChatRequest request,
            SimpMessageHeaderAccessor accessor
    ) {
        Long userId = StompSessionHelper.getUserId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));
        communityChatService.send(request.toCommand(postId, userId));
    }
}
