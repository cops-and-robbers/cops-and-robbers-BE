package com.team.cops_and_robbers.play.ping.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.constant.RedisChannel;
import com.team.cops_and_robbers.game.area.application.GameAreaService;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.ping.application.dto.PingCommand;
import com.team.cops_and_robbers.play.ping.domain.PingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("PingPublisher")
class PingPublisherTest extends ServiceUnitTest {

    @InjectMocks
    private PingPublisher pingPublisher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private InGameParticipantCacheRepository inGameParticipantCacheRepository;

    @Mock
    private GameAreaService gameAreaService;

    private static final Long GAME_ID = 1L;
    private static final Long PARTICIPANT_ID = 10L;
    private static final double INSIDE_LAT = 37.4979;
    private static final double INSIDE_LON = 127.0276;
    private static final double OUTSIDE_LAT = 38.0;
    private static final double OUTSIDE_LON = 128.0;

    private PingCommand commandInsideZone(Long participantId) {
        return new PingCommand(GAME_ID, participantId, PingType.FOUND, INSIDE_LAT, INSIDE_LON);
    }

    private InGameParticipantCache cacheOf(Team team) {
        return new InGameParticipantCache("테스터", team, "fcm-token");
    }

    @Nested
    @DisplayName("핑 유효성 검사 (DROP)")
    class SilentDrop {

        @Test
        void 참여자_캐시가_없으면_Redis_publish를_호출하지_않는다() {
            // given
            PingCommand command = commandInsideZone(PARTICIPANT_ID);
            given(inGameParticipantCacheRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.empty());

            // when
            pingPublisher.route(command);

            // then
            verify(redisTemplate, never()).convertAndSend(any(), any());
        }

        @Test
        void 구역_밖_좌표면_Redis_publish를_호출하지_않는다() {
            // given
            PingCommand command = new PingCommand(GAME_ID, PARTICIPANT_ID, PingType.FOUND, OUTSIDE_LAT, OUTSIDE_LON);
            given(inGameParticipantCacheRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(cacheOf(Team.POLICE)));
            given(gameAreaService.isInsidePlayground(GAME_ID, OUTSIDE_LON, OUTSIDE_LAT))
                    .willReturn(false);

            // when
            pingPublisher.route(command);

            // then
            verify(redisTemplate, never()).convertAndSend(any(), any());
        }
    }

    @Nested
    @DisplayName("팀별 채널 라우팅")
    class ChannelRouting {

        @Test
        void 경찰팀_참여자는_PING_POLICE_채널로_publish된다() {
            // given
            PingCommand command = commandInsideZone(PARTICIPANT_ID);
            given(inGameParticipantCacheRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(cacheOf(Team.POLICE)));
            given(gameAreaService.isInsidePlayground(GAME_ID, INSIDE_LON, INSIDE_LAT))
                    .willReturn(true);

            // when
            pingPublisher.route(command);

            // then
            verify(redisTemplate).convertAndSend(eq(RedisChannel.PING_POLICE.getTopic(GAME_ID)), any());
        }

        @Test
        void 도둑팀_참여자는_PING_ROBBER_채널로_publish된다() {
            // given
            PingCommand command = commandInsideZone(PARTICIPANT_ID);
            given(inGameParticipantCacheRepository.findByParticipantId(GAME_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(cacheOf(Team.ROBBER)));
            given(gameAreaService.isInsidePlayground(GAME_ID, INSIDE_LON, INSIDE_LAT))
                    .willReturn(true);

            // when
            pingPublisher.route(command);

            // then
            verify(redisTemplate).convertAndSend(eq(RedisChannel.PING_ROBBER.getTopic(GAME_ID)), any());
        }
    }
}
