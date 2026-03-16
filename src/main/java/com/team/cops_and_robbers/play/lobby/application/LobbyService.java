package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.lobby.application.dto.command.GameStartCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.KickCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.LobbyInfoCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.ReadyUpdateCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.TeamChangeCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.result.LobbyInfoResult;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LobbyService {

    private static final int MIN_TEAM_SIZE = 1;

    private final Clock clock;
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

        LocalDateTime now = LocalDateTime.now(clock);
        game.startGame(now);

        gameParticipantRepository.updateStatusByGameIdAndTeam(command.gameId(), Team.ROBBER, ParticipantStatus.ALIVE);
        gameParticipantRepository.updateStatusByGameIdAndTeam(command.gameId(), Team.POLICE, ParticipantStatus.POLICE_WAITING);

        LobbyEvent event = lobbyEventFactory.createGameStartEvent(command.gameId(), now);
        eventPublisher.publishEvent(event);
    }

    /**
     * 로비 조회
     */
    public LobbyInfoResult getLobbyInfo(LobbyInfoCommand command) {
        Game game = getWaitingGame(command.gameId());
        List<GameParticipant> participants = gameParticipantRepository.findAllByGameIdWithUser(command.gameId());

        GameParticipant myParticipant = findParticipantByUserId(participants, command.userId());
        GameParticipant hostParticipant = findHostParticipant(participants);

        List<LobbyInfoResult.ParticipantInfo> participantInfos = participants.stream()
                .map(LobbyInfoResult.ParticipantInfo::from)
                .toList();

        return LobbyInfoResult.of(myParticipant, hostParticipant, participantInfos);
    }

    /**
     * 강제 퇴장
     */
    @Transactional
    public void kickMember(KickCommand command) {
        Game game = getWaitingGame(command.gameId());
        GameParticipant hostParticipant = getWaitingParticipant(game.getId(), command.userId());
        GameParticipant kickParticipant = gameParticipantRepository.getByIdAndGameId(
                command.targetParticipantId(),
                command.gameId()
        );

        validateHostPermission(hostParticipant);
        validateNotSelf(hostParticipant, kickParticipant);

        gameParticipantRepository.delete(kickParticipant);

        LobbyEvent event = lobbyEventFactory.createKickEvent(command.gameId(), command.targetParticipantId());
        eventPublisher.publishEvent(event);
    }

    private GameParticipant findParticipantByUserId(List<GameParticipant> participants, Long userId) {
        return participants.stream()
                .filter(participant -> participant.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));
    }

    private GameParticipant findHostParticipant(List<GameParticipant> participants) {
        return participants.stream()
                .filter(GameParticipant::isHost)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));
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

    private void validateNotSelf(GameParticipant host, GameParticipant target) {
        if (host.getId().equals(target.getId())) {
            throw new ApplicationException(GameParticipantException.CANNOT_KICK_YOURSELF);
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
