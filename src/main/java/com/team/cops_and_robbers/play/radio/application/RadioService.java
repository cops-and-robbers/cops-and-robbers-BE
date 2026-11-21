package com.team.cops_and_robbers.play.radio.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.application.GameResultService;
import com.team.cops_and_robbers.play.radio.application.dto.command.RadioTokenCommand;
import com.team.cops_and_robbers.play.radio.application.dto.result.RadioTokenResult;
import com.team.cops_and_robbers.play.radio.domain.RadioRoom;
import com.team.cops_and_robbers.play.radio.exception.RadioException;
import com.team.cops_and_robbers.play.radio.infrastructure.RadioToken;
import com.team.cops_and_robbers.play.radio.infrastructure.RadioTokenProvider;
import com.team.cops_and_robbers.play.radio.repository.RadioOverloadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RadioService {

    private static final Duration OVERLOAD_TTL = Duration.ofMinutes(10);
    private static final Duration REISSUE_GRACE = Duration.ofMinutes(1);

    private final Clock clock;
    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;

    private final GameResultService gameResultService;
    private final RadioTokenProvider radioTokenProvider;
    private final RadioOverloadRepository radioOverloadRepository;

    /**
     * 경찰에게 무전 접속 토큰을 발급합니다. 재발급 때마다 자격을 다시 검증합니다.
     */
    @Transactional
    public RadioTokenResult issueToken(RadioTokenCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        GameParticipant police = gameParticipantRepository.getByGameIdAndUserId(
                command.gameId(), command.userId()
        );

        validateRadioAccess(game, police);
        validateRadioServerAvailable(game.getId(), command.userId());

        gameResultService.recordRadioUsage(game.getId(), command.userId(), radioTokenProvider.tokenTtl());

        String room = RadioRoom.chaserRoomOf(game.getId());
        RadioToken token = radioTokenProvider.createJoinToken(
                room, police.getId(), police.getUser().getNickname()
        );

        return RadioTokenResult.of(
                radioTokenProvider.serverUrl(), token.room(), police.getId(), token.token(), token.expiresAt()
        );
    }

    /**
     * 무전 서버 과부하 동안 신규 무전 입장을 막습니다.
     */
    public void startOverload() {
        radioOverloadRepository.activate(OVERLOAD_TTL);
        log.warn("[Radio] 무전 서버 과부하로 신규 입장 차단");
    }

    public void endOverload() {
        radioOverloadRepository.deactivate();
        log.info("[Radio] 무전 서버 과부하 해제");
    }

    private void validateRadioAccess(Game game, GameParticipant police) {
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
        if (!game.rules().allowsRadio()) {
            throw new ApplicationException(RadioException.RADIO_NOT_ALLOWED_IN_EVENT_GAME);
        }
        if (!police.isPolice()) {
            throw new ApplicationException(RadioException.ONLY_CHASER_CAN_USE_RADIO);
        }
        if (police.isPoliceWaiting()) {
            throw new ApplicationException(RadioException.RADIO_CHASER_WAITING_TIME);
        }
    }

    private void validateRadioServerAvailable(Long gameId, Long userId) {
        if (!isOverloaded() || isReissue(gameId, userId)) {
            return;
        }
        log.info("[Radio] 과부하로 신규 입장 거절 | gameId={}, userId={}", gameId, userId);
        throw new ApplicationException(RadioException.RADIO_SERVER_BUSY);
    }

    private boolean isOverloaded() {
        try {
            return radioOverloadRepository.isActive();
        } catch (DataAccessException e) {
            log.warn("[Radio] 과부하 플래그 조회 실패로 통과시킴", e);
            return false;
        }
    }

    // 재발급이 늦게 와도 무전 중이던 경찰은 통과시킨다
    private boolean isReissue(Long gameId, Long userId) {
        LocalDateTime threshold = LocalDateTime.now(clock)
                .minus(radioTokenProvider.tokenTtl())
                .minus(REISSUE_GRACE);
        return gameResultService.findRadioLastIssuedAt(gameId, userId)
                .map(issuedAt -> issuedAt.isAfter(threshold))
                .orElse(false);
    }
}
