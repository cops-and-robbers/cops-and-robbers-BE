package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.lobby.application.dto.command.ReadyUpdateCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.TeamChangeCommand;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LobbyService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;

    private final LobbyPublisher lobbyPublisher;
    private final LobbyEventFactory lobbyEventFactory;

    /**
     * 팀 변경 (경찰 <-> 도둑)
     */
    @Transactional
    public void changeTeam(TeamChangeCommand command) {
        Game game = getWaitingGame(command.gameId());
        GameParticipant participant = getWaitingParticipant(game.getId(), command.userId());

        participant.changeTeam(command.targetTeam());

        int policeCount = gameParticipantRepository.countByGameIdAndTeam(command.gameId(), Team.POLICE);
        int robberCount = gameParticipantRepository.countByGameIdAndTeam(command.gameId(), Team.ROBBER);

        LobbyEvent event = lobbyEventFactory.createTeamUpdateEvent(game.getId(), participant, policeCount, robberCount);
        lobbyPublisher.publish(event);
    }

    /**
     * 준비 상태 변경
     */
    @Transactional
    public void updateReady(ReadyUpdateCommand command) {
        Game game = getWaitingGame(command.gameId());
        GameParticipant participant = getWaitingParticipant(game.getId(), command.userId());

        participant.updateReady(command.isReady());

        LobbyEvent event = lobbyEventFactory.createReadyUpdateEvent(command.gameId(), participant);
        lobbyPublisher.publish(event);
    }

    private Game getWaitingGame(Long gameId) {
        Game game = gameRepository.getByGameId(gameId);
        if (!game.isWaiting()) {
            throw new ApplicationException(GameParticipantException.GAME_ALREADY_STARTED);
        }
        return game;
    }

    private GameParticipant getWaitingParticipant(Long gameId, Long userId){
        GameParticipant participant = gameParticipantRepository.getByGameIdAndUserId(gameId, userId);
        if (!participant.isWaiting()) {
            throw new ApplicationException(GameParticipantException.LOBBY_ACTION_NOT_ALLOWED);
        }
        return participant;
    }
}
