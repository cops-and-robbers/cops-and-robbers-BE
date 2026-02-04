package com.team.cops_and_robbers.play.location.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobberLocationService {

    private final GameParticipantRepository gameParticipantRepository;

    private final Map<Long, Map<Long, SystemEventData.RobberLocation>> locationCache = new ConcurrentHashMap<>();

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
        locationCache.computeIfAbsent(gameId, id -> new ConcurrentHashMap<>())
                .put(participantId, location);
    }

    public List<SystemEventData.RobberLocation> getCurrentRobberLocations(Long gameId) {
        Map<Long, SystemEventData.RobberLocation> locations = locationCache.get(gameId);

        if (locations == null) {
            return List.of();
        }

        return new ArrayList<>(locations.values());
    }
}
