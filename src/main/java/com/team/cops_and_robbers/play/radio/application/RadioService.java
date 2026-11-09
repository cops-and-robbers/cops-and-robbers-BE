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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RadioService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;

    private final GameResultService gameResultService;
    private final RadioTokenProvider radioTokenProvider;

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

        gameResultService.recordRadioUsage(game.getId(), command.userId(), radioTokenProvider.tokenTtl());

        String room = RadioRoom.policeRoomOf(game.getId());
        RadioToken token = radioTokenProvider.createJoinToken(
                room, police.getId(), police.getUser().getNickname()
        );

        return RadioTokenResult.of(
                radioTokenProvider.serverUrl(), room, police.getId(), token.token(), token.expiresAt()
        );
    }

    private void validateRadioAccess(Game game, GameParticipant police) {
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
        if (game.isEventGame()) {
            throw new ApplicationException(RadioException.RADIO_NOT_ALLOWED_IN_EVENT_GAME);
        }
        if (!police.isPolice()) {
            throw new ApplicationException(RadioException.ONLY_POLICE_CAN_USE_RADIO);
        }
        if (police.isPoliceWaiting()) {
            throw new ApplicationException(RadioException.RADIO_POLICE_WAITING_TIME);
        }
    }
}
