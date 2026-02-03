package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.constant.RedisChannel;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(SystemEvent systemEvent) {
        String destinationTopic = RedisChannel.SYSTEM.getTopic(systemEvent.gameId());
        redisTemplate.convertAndSend(destinationTopic, systemEvent);

        log.debug("System Pub: topic={}, type={}, eventId={}",
                destinationTopic, systemEvent.type(), systemEvent.eventId());
    }
}
