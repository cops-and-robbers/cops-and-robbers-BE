package com.team.cops_and_robbers.game.participant.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameJoinCommand;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameLeaveCommand;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameJoinResult;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameLeaveResult;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameParticipantService {

    private static final int NO_PARTICIPANTS = 0;
    
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;


    @Transactional
    public GameJoinResult joinGame(GameJoinCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        validateJoinable(command.userId(), game, command.inviteCode());

        User user = userRepository.getByUserId(command.userId());
        GameParticipant participant = GameParticipant.createParticipant(game, user, false);
        gameParticipantRepository.save(participant);

        // TODO : ParticipantJoined 도메인 이벤트 publish

        return GameJoinResult.from(participant);
    }

    private void validateJoinable(Long userId, Game game, String inviteCode) {
        if (gameParticipantRepository.existsActiveGameByUserId(userId)) {
            throw new ApplicationException(GameParticipantException.ALREADY_PARTICIPATING);
        }

        if (game.isWaiting()) {
            throw new ApplicationException(GameParticipantException.GAME_ALREADY_STARTED);
        }

        if (!game.isSameInviteCode(inviteCode)) {
            throw new ApplicationException(GameParticipantException.INVALID_INVITE_CODE);
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
        gameParticipantRepository.delete(participant);

        // 1. 마지막 사람이 나갈 경우 방을 삭제한다.
        int remainingCount = gameParticipantRepository.countByGameId(command.gameId());
        if (remainingCount == NO_PARTICIPANTS) {
            gameAreaRepository.deleteByGameId(command.gameId());
            gameRepository.deleteById(command.gameId());

            // TODO : GameDeleted 도메인 이벤트 publish

            return GameLeaveResult.from(command.userId(), NO_PARTICIPANTS);
        }

        // 2. 방장이 나갈 경우 방장 다음 들어온 사람에게 방장을 위임한다.
        if (wasHost) {
            GameParticipant newHost = gameParticipantRepository.getNextParticipant(command.gameId());
            newHost.promoteToHost();

            // TODO : HostChanged 도메인 이벤트 publish
        }

        // TODO : ParticipantLeft 도메인 이벤트 publish

        return GameLeaveResult.from(command.userId(), remainingCount);
    }

    private void validateLeavable(GameParticipant participant) {
        if (participant.isWaiting()) {
            throw new ApplicationException(GameParticipantException.CANNOT_LEAVE_DURING_GAME);
        }
    }
}
