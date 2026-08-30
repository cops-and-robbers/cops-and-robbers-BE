package com.team.cops_and_robbers.community.chat.message.application;

import com.team.cops_and_robbers.common.constant.RedisChannel;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityChatPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(CommunityChatMessage message) {
        CommunityChatPayload payload = CommunityChatPayload.from(message);
        String destinationTopic = RedisChannel.COMMUNITY_CHAT.getTopic(message.getCommunityPostId());

        redisTemplate.convertAndSend(destinationTopic, payload);

        log.debug("[CommunityChat] Pub: topic={}, senderId={}", destinationTopic, message.getSenderId());
    }
}
