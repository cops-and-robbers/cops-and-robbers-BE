package com.team.cops_and_robbers.common.presentation;

import com.team.cops_and_robbers.common.util.StompPathUtil;
import com.team.cops_and_robbers.community.chat.common.presentation.CommunityStompInterceptor;
import com.team.cops_and_robbers.play.common.GameStompInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * destination을 보고 담당 기능의 인터셉터에 위임한다.
 * 각 인터셉터는 자기 경로의 프레임만 받는다는 것을 전제할 수 있다.
 * 인증(CONNECT)은 앞단의 StompAuthInterceptor가 끝낸 뒤이므로 여기서는 다루지 않는다.
 */
@Component
@RequiredArgsConstructor
public class RoutingStompInterceptor implements ChannelInterceptor {

    private final GameStompInterceptor gameStompInterceptor;
    private final CommunityStompInterceptor communityStompInterceptor;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String destination = accessor.getDestination();

        if (destination == null) {
            return message;
        }

        if (StompPathUtil.isCommunityPath(destination) || StompPathUtil.isUserPath(destination)) {
            return communityStompInterceptor.preSend(message, channel);
        }
        return gameStompInterceptor.preSend(message, channel);
    }
}
