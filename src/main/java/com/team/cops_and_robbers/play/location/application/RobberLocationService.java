package com.team.cops_and_robbers.play.location.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.location.application.dto.command.RobberLocationsCommand;
import com.team.cops_and_robbers.play.location.application.dto.result.RobberLocationResult;
import com.team.cops_and_robbers.play.location.repository.RobberLocationRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobberLocationService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final RobberLocationRepository robberLocationRepository;

    public void updateLocation(LocationUpdateCommand command) {
        Optional<GameParticipant> optionalParticipant =
                gameParticipantRepository.findByIdWithUser(command.participantId());
        if (optionalParticipant.isEmpty()) return;

        GameParticipant participant = optionalParticipant.get();
        if (participant.getTeam() != Team.ROBBER) {
            throw new ApplicationException(GameParticipantException.NOT_ROBBER_TEAM);
        }

        SystemEventData.RobberLocation location =
                SystemEventData.RobberLocation.of(
                        command.participantId(),
                        participant.getUser().getNickname(),
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
        gameRepository.getByGameId(command.gameId());
        gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.userId());

        return getCurrentRobberLocations(command.gameId()).stream()
                .map(RobberLocationResult::from)
                .toList();
    }
}