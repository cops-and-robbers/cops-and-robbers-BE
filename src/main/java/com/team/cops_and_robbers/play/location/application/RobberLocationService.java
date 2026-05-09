package com.team.cops_and_robbers.play.location.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.location.application.dto.command.RobberLocationsCommand;
import com.team.cops_and_robbers.play.location.application.dto.result.RobberLocationResult;
import com.team.cops_and_robbers.play.location.repository.RobberLocationRepository;
import com.team.cops_and_robbers.play.location.repository.RobberLocationSnapshotRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobberLocationService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final RobberLocationRepository robberLocationRepository;
    private final RobberLocationSnapshotRepository robberLocationSnapshotRepository;
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

        robberLocationRepository.save(command.gameId(), command.participantId(), location);
    }

    /**
     * 도둑 위치 조회 (주기적 공개 스케줄러 용)
     *   - ALIVE 상태의 도둑 위치만 조회
     *   - 조회 후, 스냅샷 생성
     */
    public List<SystemEventData.RobberLocation> revealRobberLocations(Long gameId) {
        Set<Long> aliveRobberIds = gameParticipantRepository.findAliveRobberIdsByGameId(gameId);
        List<SystemEventData.RobberLocation> locations = robberLocationRepository.findAllByGameId(gameId).stream()
                .filter(loc -> aliveRobberIds.contains(loc.participantId()))
                .toList();
        robberLocationSnapshotRepository.saveAll(gameId, locations);
        return locations;
    }

    public void clearRobberLocations(Long gameId) {
        robberLocationRepository.deleteAllByGameId(gameId);
    }

    public void clearRobberLocationSnapshot(Long gameId) {
        robberLocationSnapshotRepository.deleteAllByGameId(gameId);
    }

    @Deprecated
    public List<RobberLocationResult> getRobberLocations(RobberLocationsCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
        gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.userId());

        Set<Long> aliveRobberIds = gameParticipantRepository.findAliveRobberIdsByGameId(command.gameId());
        return robberLocationSnapshotRepository.findAllByGameId(command.gameId()).stream()
                .filter(loc -> aliveRobberIds.contains(loc.participantId()))
                .map(RobberLocationResult::from)
                .toList();
    }
}
