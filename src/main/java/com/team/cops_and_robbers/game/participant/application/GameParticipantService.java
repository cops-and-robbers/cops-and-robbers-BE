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
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.application.GameResultService;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.lobby.application.LobbyEventFactory;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.play.system.application.GameTerminationService;
import com.team.cops_and_robbers.play.system.application.SystemEventFactory;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
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
    private final InGameParticipantCacheRepository inGameParticipantCacheRepository;

    private final ApplicationEventPublisher eventPublisher;
    private final LobbyEventFactory lobbyEventFactory;
    private final SystemEventFactory systemEventFactory;
    private final GameTerminationService gameTerminationService;
    private final GameResultService gameResultService;

    @Transactional
    public GameJoinResult joinGame(GameJoinCommand command) {
        Game game = gameRepository.getByInviteCode(command.inviteCode());

        if (game.isEventGame()) {
            if (gameParticipantRepository.existsActiveGameByUserId(command.userId())) {
                throw new ApplicationException(GameParticipantException.ALREADY_PARTICIPATING);
            }
            if (!game.isInProgress()) {
                throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
            }
            User eventUser = userRepository.getByUserId(command.userId());
            GameParticipant eventParticipant = GameParticipant.createEventModeParticipant(game, eventUser);
            gameParticipantRepository.save(eventParticipant);
            gameResultService.recordParticipantJoined(game.getId(), eventParticipant);
            return GameJoinResult.from(eventParticipant);
        }

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

    /**
     * 1. 게임방 나가기 기능을 처리합니다.
     * - 로비 상태 일떄 (게임 시작 x)
     * - 게임 중인 상태일때
     */
    @Transactional
    public GameLeaveResult leaveGame(GameLeaveCommand command) {
        GameParticipant participant = gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.userId());
        Game game = participant.getGame();

        if (game.isInProgress()) {
            return leaveFromInGame(participant, game, command.gameId(), command.userId());
        }
        return leaveFromLobby(participant, command.gameId(), command.userId());
    }

    /**
     * 1-1. 로비 상태의 나가기를 처리합니다. (게임 중 x)
     * - 로비에 남은 인원이 없을땐 게임방을 삭제 합니다.
     * - 방장이 퇴장 할땐 방장을 넘겨준 뒤 퇴장합니다.
     */
    private GameLeaveResult leaveFromLobby(GameParticipant participant, Long gameId, Long userId) {
        boolean wasHost = participant.isHost();
        Long exitedParticipantId = participant.getId();
        int maxCount = participant.getGame().getMaxParticipants();

        gameParticipantRepository.delete(participant);

        int remainingCount = gameParticipantRepository.countByGameId(gameId);
        if (remainingCount == NO_PARTICIPANTS) {
            gameAreaRepository.deleteByGameId(gameId);
            gameRepository.deleteById(gameId);
            return GameLeaveResult.from(userId, NO_PARTICIPANTS);
        }

        transferHostIfNeeded(wasHost, gameId);

        LobbyEvent exitEvent = lobbyEventFactory.createExitEvent(gameId, exitedParticipantId, remainingCount, maxCount);
        eventPublisher.publishEvent(exitEvent);

        return GameLeaveResult.from(userId, remainingCount);
    }

    /**
     * 1-2. 인게임 중 나가기를 처리합니다.
     * - 경찰 퇴장
     *      - 마지막 경찰이면 도둑 팀 승리로 게임 종료 (POLICE_FORFEITED)
     *      - 경찰이 남아있으면 PLAYER_LEFT 이벤트 발행
     * - 도둑 퇴장
     *      - ALIVE 상태이고 마지막 생존 도둑이면 경찰 팀 승리로 게임 종료 (ROBBER_FORFEITED)
     *      - JAILED 상태이거나 생존 도둑이 남아있으면 PLAYER_LEFT 이벤트 발행
     */
    private GameLeaveResult leaveFromInGame(GameParticipant participant, Game game, Long gameId, Long userId) {
        Team team = participant.getTeam();
        ParticipantStatus status = participant.getStatus();
        boolean wasHost = participant.isHost();

        SystemEvent playerLeftEvent = systemEventFactory.createPlayerLeftEvent(gameId, participant);
        gameResultService.recordParticipantLeft(gameId, userId);
        removeParticipant(gameId, participant);

        int remainingCount = gameParticipantRepository.countByGameId(gameId);

        transferHostIfNeeded(wasHost && remainingCount > NO_PARTICIPANTS, gameId);

        eventPublisher.publishEvent(playerLeftEvent);

        tryEndGameByForfeit(game, gameId, team, status);
        return GameLeaveResult.from(userId, remainingCount);
    }

    private void transferHostIfNeeded(boolean wasHost, Long gameId) {
        if (!wasHost) return;
        GameParticipant newHost = gameParticipantRepository.getNextParticipant(gameId);
        newHost.promoteToHost();
        eventPublisher.publishEvent(lobbyEventFactory.createHostChangedEvent(gameId, newHost));
    }

    private boolean tryEndGameByForfeit(Game game, Long gameId, Team team, ParticipantStatus status) {
        if (team == Team.POLICE && !game.isEventGame() && isLastPolice(gameId)) {
            gameTerminationService.endGameByPoliceForfeited(gameId);
            return true;
        }
        if (team == Team.ROBBER && status == ParticipantStatus.ALIVE && isLastAliveRobber(gameId)) {
            gameTerminationService.endGameByRobberForfeited(gameId);
            return true;
        }
        return false;
    }

    private void removeParticipant(Long gameId, GameParticipant participant) {
        inGameParticipantCacheRepository.deleteByParticipantId(gameId, participant.getId());
        gameParticipantRepository.delete(participant);
    }

    private boolean isLastPolice(Long gameId) {
        return gameParticipantRepository.countByGameIdAndTeam(gameId, Team.POLICE) == NO_PARTICIPANTS;
    }

    private boolean isLastAliveRobber(Long gameId) {
        return gameParticipantRepository.countByGameIdAndRobberStatus(gameId, ParticipantStatus.ALIVE) == NO_PARTICIPANTS;
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
