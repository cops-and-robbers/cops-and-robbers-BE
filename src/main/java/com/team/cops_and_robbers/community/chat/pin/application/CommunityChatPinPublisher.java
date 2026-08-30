package com.team.cops_and_robbers.community.chat.pin.application;

import com.team.cops_and_robbers.common.constant.RedisChannel;
import com.team.cops_and_robbers.community.chat.pin.application.event.CommunityChatPinChangedEvent;
import com.team.cops_and_robbers.community.chat.pin.domain.CommunityChatPin;
import com.team.cops_and_robbers.community.chat.pin.domain.CommunityChatPinPayload;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserProfileProjection;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityChatPinPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    /**
     * 멤버 목록 조회와 같은 방식으로 배너 갱신 시점의 현재 닉네임을 조회한다.
     */
    public void publish(CommunityChatPinChangedEvent event) {
        CommunityChatPin pin = event.pin();
        User writer = userRepository.findById(pin.getWriterId()).orElse(null);
        UserProfileProjection writerProfile = UserProfileProjection.of(pin.getWriterId(), writer);

        CommunityChatPinPayload payload = CommunityChatPinPayload.of(pin, event.action(), writerProfile.nickname());
        String destinationTopic = RedisChannel.COMMUNITY_CHAT_PIN.getTopic(pin.getCommunityPostId());

        redisTemplate.convertAndSend(destinationTopic, payload);

        log.debug("[CommunityChatPin] Pub: topic={}, action={}", destinationTopic, event.action());
    }
}
