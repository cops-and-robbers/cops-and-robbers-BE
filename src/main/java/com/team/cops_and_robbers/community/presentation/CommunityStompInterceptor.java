package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.util.StompPathUtil;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * 커뮤니티 채팅 경로(/publish/community/**, /subscribe/community/**)의 접근 권한을 검증한다.
 * CONNECT 인증은 StompAuthInterceptor가 담당하므로 여기서는 다루지 않으며,
 * 그때 세션에 저장된 userId를 꺼내 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityStompInterceptor implements ChannelInterceptor {

    private final CommunityChatMemberRepository communityChatMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand stompCommand = accessor.getCommand();

        if (stompCommand == null) return message;

        if (stompCommand == StompCommand.SUBSCRIBE && StompPathUtil.isUserPath(accessor.getDestination())) {
            return isOwnChannel(accessor) ? message : null;
        }

        switch (stompCommand) {
            case SUBSCRIBE -> handleSubscribe(accessor);
            case SEND -> handlePublish(accessor);
        }

        return message;
    }

    /**
     * 1. [SUBSCRIBE]: 채팅방 참여 권한 검증
     * - 소켓 세션의 유저 정보와 구독 경로의 게시글 ID로 채팅방 멤버인지 검증합니다.
     * (DB 조회)
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        Long userId = getUserIdFromSession(accessor);
        Long postId = getPostIdFromPath(accessor);

        validateChatMember(postId, userId);
        log.info("[CommunityChat] SUBSCRIBE success: postId={}, userId={}", postId, userId);
    }

    /**
     * 유저 단위 채널은 내 모든 방의 메시지가 흐르므로 채널 주인이 본인인지로 판별한다.
     * 자격이 없으면 예외 대신 프레임을 버려 구독만 막는다.
     * 게임과 소켓을 공유하고 있어, ERROR 프레임으로 연결을 닫으면 인게임까지 끊긴다.
     */
    private boolean isOwnChannel(StompHeaderAccessor accessor) {
        Long userId = getUserIdFromSession(accessor);
        Long channelOwnerId = StompPathUtil.getUserId(accessor.getDestination())
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_DESTINATION));

        if (!channelOwnerId.equals(userId)) {
            log.warn("[CommunityChat] Unauthorized channel access. ownerId={}, userId={}", channelOwnerId, userId);
            return false;
        }
        log.info("[CommunityChat] SUBSCRIBE success: userChannel={}", userId);
        return true;
    }

    /**
     * 2. [SEND]: 채팅방 참여 권한 검증
     * - 게임과 달리 세션에 채팅방 정보를 캐싱하지 않고 전송마다 검증합니다.
     * 한 소켓 세션으로 여러 채팅방을 동시에 구독할 수 있어, 세션에 postId 하나를 담아두면
     * 두 번째 방의 전송이 오탐되기 때문입니다.
     * (DB 조회)
     */
    private void handlePublish(StompHeaderAccessor accessor) {
        Long userId = getUserIdFromSession(accessor);
        Long postId = getPostIdFromPath(accessor);

        validateChatMember(postId, userId);
    }

    private void validateChatMember(Long postId, Long userId) {
        if (!communityChatMemberRepository.existsByCommunityPostIdAndUserId(postId, userId)) {
            log.warn("[CommunityChat] Unauthorized chat access. postId={}, userId={}", postId, userId);
            throw new ApplicationException(CommunityChatException.NOT_A_CHAT_MEMBER);
        }
    }

    private Long getUserIdFromSession(StompHeaderAccessor accessor) {
        return StompSessionHelper.getUserId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));
    }

    private Long getPostIdFromPath(StompHeaderAccessor accessor) {
        return StompPathUtil.getPostId(accessor.getDestination())
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_DESTINATION));
    }
}
