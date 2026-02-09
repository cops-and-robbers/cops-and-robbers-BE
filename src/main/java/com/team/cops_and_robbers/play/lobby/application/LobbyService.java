package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.lobby.application.dto.command.GameStartCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.ReadyUpdateCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.TeamChangeCommand;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LobbyService {

    private static final int MIN_TEAM_SIZE = 1;

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;

    private final ApplicationEventPublisher eventPublisher;
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
        eventPublisher.publishEvent(event);
    }

    /**
     * 준비 상태 변경
     */
    @Transactional
    public void updateReady(ReadyUpdateCommand command) {
        Game game = getWaitingGame(command.gameId());
        GameParticipant participant = getWaitingParticipant(game.getId(), command.userId());

        if (participant.isHost()) {
            throw new ApplicationException(GameParticipantException.HOST_CANNOT_UNREADY);
        }

        participant.updateReady(command.isReady());

        LobbyEvent event = lobbyEventFactory.createReadyUpdateEvent(command.gameId(), participant);
        eventPublisher.publishEvent(event);
    }

    /**
     * 게임 시작
     */
    @Transactional
    public void startGame(GameStartCommand command) {
        Game game = getWaitingGame(command.gameId());
        GameParticipant participant = getWaitingParticipant(game.getId(), command.userId());

        validateGameStart(game, participant);

        LocalDateTime nowKst = TimestampUtil.nowKstLocal();
        game.startGame(nowKst);
        gameParticipantRepository.updateStatusByGameId(command.gameId(), ParticipantStatus.ALIVE);

        LobbyEvent event = lobbyEventFactory.createGameStartEvent(command.gameId(), nowKst);
        eventPublisher.publishEvent(event);
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

    private void validateGameStart(Game game, GameParticipant participant) {
        validateHostPermission(participant);
        validateTeamComposition(game.getId());
        validateAllParticipantsReady(game.getId());
    }

    private void validateHostPermission(GameParticipant participant) {
        if (!participant.isHost()) {
            throw new ApplicationException(GameParticipantException.NOT_HOST);
        }
    }

    private void validateTeamComposition(Long gameId) {
        int policeCount = gameParticipantRepository.countByGameIdAndTeam(gameId, Team.POLICE);
        int robberCount = gameParticipantRepository.countByGameIdAndTeam(gameId, Team.ROBBER);
        if (policeCount < MIN_TEAM_SIZE || robberCount < MIN_TEAM_SIZE) {
            throw new ApplicationException(GameParticipantException.INVALID_TEAM_COMPOSITION);
        }
    }

    private void validateAllParticipantsReady(Long gameId) {
        if (gameParticipantRepository.existsNotReadyParticipantByGameId(gameId)) {
            throw new ApplicationException(GameParticipantException.NOT_ALL_READY);
        }
    }
}
