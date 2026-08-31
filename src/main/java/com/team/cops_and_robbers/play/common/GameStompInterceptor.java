package com.team.cops_and_robbers.play.common;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.util.StompPathUtil;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * 게임 경로(/publish/game/**, /subscribe/game/**)의 접근 권한을 검증한다.
 * CONNECT 인증은 StompAuthInterceptor가 담당하므로 여기서는 다루지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameStompInterceptor implements ChannelInterceptor {

    private static final String POLICE_CHANNEL_SUFFIX = "/police";
    private static final String ROBBER_CHANNEL_SUFFIX = "/robber";

    private final GameParticipantRepository gameParticipantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand stompCommand = accessor.getCommand();

        if (stompCommand == null) return message;

        switch (stompCommand) {
            case SUBSCRIBE -> handleSubscribe(accessor);
            case SEND -> handlePublish(accessor);
        }

        return message;
    }

    /**
     * 1. [SUBSCRIBE]: 방 참여 권한 검증 & 게임방 정보 세션 캐싱
     * - 소켓 세션의 유저 정보와 구독 경로의 게임방 ID로 참여 권한을 검증하고,
     * 검증된 게임방 정보를 소켓 세션에 저장합니다.
     * (추후 redis 조회로 변경 예정)
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        Long userId = getUserIdFromSession(accessor);
        Long gameId = getGameIdFromPath(accessor);
        GameParticipant participant = getParticipant(gameId, userId);

        validateSubscriptionPermission(accessor.getDestination(), participant.getTeam());

        StompSessionHelper.putGameId(accessor, gameId);
        StompSessionHelper.putParticipantId(accessor, participant.getId());
        log.info("[Wed Socket] SUBSCRIBE success: gameId={}, userId={}, participantId={}", gameId, userId, participant.getId());
    }

    /**
     * 2. [SEND]: 방 참여 권한 검증
     * - 소켓 세션의 유저, 게임방 정보로 publish 권한을 검증합니다.
     */
    private void handlePublish(StompHeaderAccessor accessor) {
        Long sessionGameId = getGameIdFromSession(accessor);
        Long targetGameId = getGameIdFromPath(accessor);

        if (!sessionGameId.equals(targetGameId)) {
            throw new ApplicationException(GameParticipantException.NOT_A_PARTICIPANT);
        }
    }

    private void validateSubscriptionPermission(String destination, Team participantTeam) {
        if (destination.endsWith(POLICE_CHANNEL_SUFFIX)) {
            if (participantTeam != Team.POLICE) {
                log.warn("[Wed Socket] Unauthorized subscription attempt to POLICE channel. Actual participant Team: {}", participantTeam);
                throw new ApplicationException(CommonException.UNAUTHORIZED_SUBSCRIPTION);
            }
        }

        else if (destination.endsWith(ROBBER_CHANNEL_SUFFIX)) {
            if (participantTeam != Team.ROBBER) {
                log.warn("[Wed Socket] Unauthorized subscription attempt to ROBBER channel. Actual participant Team: {}", participantTeam);
                throw new ApplicationException(CommonException.UNAUTHORIZED_SUBSCRIPTION);
            }
        }
    }

    private Long getUserIdFromSession(StompHeaderAccessor accessor) {
        return StompSessionHelper.getUserId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));
    }

    private Long getGameIdFromPath(StompHeaderAccessor accessor) {
        return StompPathUtil.getGameId(accessor.getDestination())
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_DESTINATION));
    }

    private Long getGameIdFromSession(StompHeaderAccessor accessor) {
        return StompSessionHelper.getGameId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_DESTINATION));
    }

    private GameParticipant getParticipant(Long gameId, Long userId) {
        return gameParticipantRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new ApplicationException(GameParticipantException.NOT_A_PARTICIPANT));
    }
}
