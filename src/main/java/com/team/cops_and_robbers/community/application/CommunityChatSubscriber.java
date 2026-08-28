package com.team.cops_and_robbers.community.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.constant.StompSubscribeChannel;
import com.team.cops_and_robbers.community.domain.CommunityChatPayload;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityChatSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;
    private final CommunityChatMemberRepository communityChatMemberRepository;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CommunityChatPayload payload = objectMapper.readValue(message.getBody(), CommunityChatPayload.class);
            String destination = StompSubscribeChannel.COMMUNITY_CHAT.getUrl(payload.communityPostId());

            messagingTemplate.convertAndSend(destination, payload);
            sendToMemberChannels(payload);
        } catch (Exception e) {
            log.error("[CommunityChatSubscriber] Message processing failed", e);
        }
    }

    /**
     * 채팅방 목록 화면은 방마다 구독하지 않으므로, 멤버별 채널로 한 번 더 보내 목록을 갱신한다.
     * 안 읽은 개수는 담지 않는다. 수신자마다 세면 DB 조회가 인원수만큼 늘어나므로 앱이 직접 올린다.
     */
    private void sendToMemberChannels(CommunityChatPayload payload) {
        List<Long> memberIds = communityChatMemberRepository
                .findUserIdsByCommunityPostId(payload.communityPostId());

        memberIds.forEach(memberId ->
                messagingTemplate.convertAndSend(
                        StompSubscribeChannel.COMMUNITY_CHAT_USER.getUrl(memberId), payload));
    }
}