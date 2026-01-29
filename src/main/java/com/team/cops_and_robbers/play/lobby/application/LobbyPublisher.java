package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.common.constant.RedisChannel;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(LobbyEvent lobbyEvent) {
        String destinationTopic = RedisChannel.LOBBY.getTopic(lobbyEvent.gameId());
        redisTemplate.convertAndSend(destinationTopic, lobbyEvent);

        log.debug("Lobby Pub: topic={}, type={}, eventId={}",
                destinationTopic, lobbyEvent.type(), lobbyEvent.eventId());
    }
}
