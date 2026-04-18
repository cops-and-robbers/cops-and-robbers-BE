package com.team.cops_and_robbers.play.location.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.location.application.dto.command.RobberLocationsCommand;
import com.team.cops_and_robbers.play.location.application.dto.result.RobberLocationResult;
import com.team.cops_and_robbers.play.location.repository.RobberLocationRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobberLocationService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final RobberLocationRepository robberLocationRepository;
    private final InGameParticipantCacheRepository inGameParticipantCacheRepository;

    public void updateLocation(LocationUpdateCommand command) {
        InGameParticipantCache participant = inGameParticipantCacheRepository.findByParticipantId(command.gameId(), command.participantId())
                .orElseThrow(() -> new ApplicationException(GameParticipantException.NOT_A_PARTICIPANT));

        if (participant.team() != Team.ROBBER) {
            throw new ApplicationException(GameParticipantException.NOT_ROBBER_TEAM);
        }

        SystemEventData.RobberLocation location =
                SystemEventData.RobberLocation.of(
                        command.participantId(),
                        participant.nickname(),
                        command.latitude(),
                        command.longitude()
                );

        storeRobberLocation(command.gameId(), command.participantId(), location);
    }

    private void storeRobberLocation(
            Long gameId,
            Long participantId,
            SystemEventData.RobberLocation location
    ) {
        robberLocationRepository.save(gameId, participantId, location);
    }

    public List<SystemEventData.RobberLocation> getCurrentRobberLocations(Long gameId) {
        return robberLocationRepository.findAllByGameId(gameId);
    }

    public void clearRobberLocations(Long gameId) {
        robberLocationRepository.deleteAllByGameId(gameId);
    }

    public List<RobberLocationResult> getRobberLocations(RobberLocationsCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
        GameParticipant participant = gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.userId());
        if (participant.isPoliceWaiting()) {
            throw new ApplicationException(GameParticipantException.POLICE_WAITING_TIME);
        }

        return getCurrentRobberLocations(command.gameId()).stream()
                .map(RobberLocationResult::from)
                .toList();
    }
}
