package com.team.cops_and_robbers.play;

import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.util.AuthorizationExtractor;
import com.team.cops_and_robbers.common.util.StompPathUtil;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.exception.UserException;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private static final String POLICE_CHANNEL_SUFFIX = "/police";
    private static final String ROBBER_CHANNEL_SUFFIX = "/robber";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final GameParticipantRepository gameParticipantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand stompCommand = accessor.getCommand();

        if (StompCommand.CONNECT.equals(stompCommand)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(stompCommand)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(stompCommand)) {
            handlePublish(accessor);
        }

        return message;
    }

    /**
     * 1. [CONNECT]: 토큰 검증 & 유저 정보 세션 캐싱
     * - web socket 연결한 사용자를 검증하고, 소켓 세션에 유저 정보를 저장합니다.
     * (DB 조회)
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String accessToken = AuthorizationExtractor.extractToken(accessor)
                .orElseThrow(() -> new ApplicationException(AuthException.UNAUTHENTICATED_REQUEST));
        Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);

        if (!userRepository.existsById(userId)) {
            throw new ApplicationException(UserException.USER_NOT_FOUND);
        }

        StompSessionHelper.putUserId(accessor, userId);
        log.info("[Wed Socket] CONNECT success: userId={}", userId);
    }

    /**
     * 2. [SUBSCRIBE]: 방 참여 권한 검증 & 게임방 정보 세션 캐싱
     * - 소켓 세션의 유저 정보와 구독 경로의 게임방 ID로 참여 권한을 검증하고,
     * 검증된 게임방 정보를 소켓 세션에 저장합니다.
     * (추후 redis 조회로 변경 예정)
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        Long userId = StompSessionHelper.getUserId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));
        Long gameId = StompPathUtil.getGameId(accessor.getDestination())
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_DESTINATION));
        GameParticipant participant = gameParticipantRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new ApplicationException(GameParticipantException.NOT_A_PARTICIPANT));

        validateSubscriptionPermission(accessor.getDestination(), participant.getTeam());

        StompSessionHelper.putGameId(accessor, gameId);
        StompSessionHelper.putParticipantId(accessor, participant.getId());
        log.info("[Wed Socket] SUBSCRIBE success: gameId={}, userId={}, participantId={}", gameId, userId, participant.getId());
    }

    /**
     * 3. [SEND]: 방 참여 권한 검증
     * - 소켓 세션의 유저, 게임방 정보로 publish 권한을 검증합니다.
     */
    private void handlePublish(StompHeaderAccessor accessor) {
        Long sessionGameId = StompSessionHelper.getGameId(accessor)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_SOCKET_SESSION));
        Long targetGameId = StompPathUtil.getGameId(accessor.getDestination())
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_DESTINATION));

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
}
