package com.team.cops_and_robbers.game.participant.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameJoinCommand;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameLeaveCommand;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameParticipantListCommand;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameJoinResult;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameLeaveResult;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameParticipantListResult;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.lobby.application.LobbyEventFactory;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameParticipantService {

    private static final int NO_PARTICIPANTS = 0;

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;

    private final ApplicationEventPublisher eventPublisher;
    private final LobbyEventFactory lobbyEventFactory;

    @Transactional
    public GameJoinResult joinGame(GameJoinCommand command) {
        Game game = gameRepository.getByInviteCode(command.inviteCode());
        validateJoinable(command.userId(), game);

        User user = userRepository.getByUserId(command.userId());
        GameParticipant participant = GameParticipant.createParticipant(game, user, false);
        gameParticipantRepository.save(participant);

        int currentCount = gameParticipantRepository.countByGameId(game.getId());
        int maxCount = game.getMaxParticipants();

        LobbyEvent enterEvent = lobbyEventFactory.createEnterEvent(game.getId(), participant, currentCount, maxCount);
        eventPublisher.publishEvent(enterEvent);

        return GameJoinResult.from(participant);
    }

    private void validateJoinable(Long userId, Game game) {
        if (gameParticipantRepository.existsActiveGameByUserId(userId)) {
            throw new ApplicationException(GameParticipantException.ALREADY_PARTICIPATING);
        }

        if (!game.isWaiting()) {
            throw new ApplicationException(GameParticipantException.GAME_ALREADY_STARTED);
        }

        int participantCount = gameParticipantRepository.countByGameId(game.getId());
        if (participantCount >= game.getMaxParticipants()) {
            throw new ApplicationException(GameParticipantException.GAME_FULL);
        }
    }

    @Transactional
    public GameLeaveResult leaveGame(GameLeaveCommand command) {
        GameParticipant participant = gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.userId());
        validateLeavable(participant);

        boolean wasHost = participant.isHost();

        Long exitedParticipantId = participant.getId();
        int maxCount = participant.getGame().getMaxParticipants();

        gameParticipantRepository.delete(participant);

        // 1. 마지막 사람이 나갈 경우 방을 삭제한다.
        int remainingCount = gameParticipantRepository.countByGameId(command.gameId());
        if (remainingCount == NO_PARTICIPANTS) {
            gameAreaRepository.deleteByGameId(command.gameId());
            gameRepository.deleteById(command.gameId());

            return GameLeaveResult.from(command.userId(), NO_PARTICIPANTS);
        }

        // 2. 방장이 나갈 경우 방장 다음 들어온 사람에게 방장을 위임한다.
        if (wasHost) {
            GameParticipant newHost = gameParticipantRepository.getNextParticipant(command.gameId());
            newHost.promoteToHost();

            LobbyEvent hostEvent = lobbyEventFactory.createHostChangedEvent(command.gameId(), newHost);
            eventPublisher.publishEvent(hostEvent);
        }

        LobbyEvent exitEvent = lobbyEventFactory.createExitEvent(command.gameId(), exitedParticipantId, remainingCount, maxCount);
        eventPublisher.publishEvent(exitEvent);

        return GameLeaveResult.from(command.userId(), remainingCount);
    }

    private void validateLeavable(GameParticipant participant) {
        if (!participant.isWaiting()) {
            throw new ApplicationException(GameParticipantException.CANNOT_LEAVE_DURING_GAME);
        }
    }

    public GameParticipantListResult getParticipantList(GameParticipantListCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());

        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }

        gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.userId());
        List<GameParticipant> participants = gameParticipantRepository.findAllByGameIdWithUser(command.gameId());
        return GameParticipantListResult.from(participants);
    }
}
