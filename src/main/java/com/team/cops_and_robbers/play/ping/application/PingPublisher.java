package com.team.cops_and_robbers.play.ping.application;

import com.team.cops_and_robbers.common.constant.RedisChannel;
import com.team.cops_and_robbers.game.area.application.GameAreaService;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.ping.application.dto.PingCommand;
import com.team.cops_and_robbers.play.ping.domain.PingLocation;
import com.team.cops_and_robbers.play.ping.domain.PingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PingPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final InGameParticipantCacheRepository inGameParticipantCacheRepository;
    private final GameAreaService gameAreaService;

    /**
     * 1. 핑 요청을 라우팅 합니다.
     *   올바르지 않은 핑은 무시합니다.(브로드캐스트 x)
     *     - 캐시에 적재 x -> 게임이 진행 중이지 않거나, 게임 참여자가 아님
     *     - 핑을 보낸 위치가 구역 밖일때
     */
    public void route(PingCommand command) {
        findParticipant(command.gameId(), command.participantId())
                .filter(ignored -> isInsidePlayground(command))
                .ifPresent(participant -> publishPing(command, participant));
    }

    private Optional<InGameParticipantCache> findParticipant(Long gameId, Long participantId) {
        Optional<InGameParticipantCache> result =
                inGameParticipantCacheRepository.findByParticipantId(gameId, participantId);
        if (result.isEmpty()) {
            log.debug("Ping silent drop: participant not in cache, participantId={}", participantId);
        }
        return result;
    }

    private boolean isInsidePlayground(PingCommand command) {
        boolean inside = gameAreaService.isInsidePlayground(
                command.gameId(), command.longitude(), command.latitude());
        if (!inside) {
            log.debug("Ping silent drop: out of zone, gameId={}, lat={}, lng={}",
                    command.gameId(), command.latitude(), command.longitude());
        }
        return inside;
    }

    private void publishPing(PingCommand command, InGameParticipantCache participant) {
        PingMessage message = buildMessage(command, participant);
        RedisChannel channel = channelFor(participant.team());
        redisTemplate.convertAndSend(channel.getTopic(command.gameId()), message);
        log.debug("Ping published: channel={}, type={}, sender={}",
                channel.getTopic(command.gameId()), command.pingType(), participant.nickname());
    }

    private PingMessage buildMessage(PingCommand command, InGameParticipantCache participant) {
        return PingMessage.of(
                command.gameId(),
                command.pingType(),
                new PingLocation(command.latitude(), command.longitude()),
                command.participantId(),
                participant.nickname()
        );
    }

    private RedisChannel channelFor(Team team) {
        return switch (team) {
            case POLICE -> RedisChannel.PING_POLICE;
            case ROBBER -> RedisChannel.PING_ROBBER;
        };
    }
}
