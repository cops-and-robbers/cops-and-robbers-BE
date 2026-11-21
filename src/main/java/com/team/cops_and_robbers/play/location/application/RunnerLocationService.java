package com.team.cops_and_robbers.play.location.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.location.repository.RunnerLocationRepository;
import com.team.cops_and_robbers.play.location.repository.RunnerLocationSnapshotRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunnerLocationService {

    private final GameParticipantRepository gameParticipantRepository;
    private final RunnerLocationRepository runnerLocationRepository;
    private final RunnerLocationSnapshotRepository runnerLocationSnapshotRepository;
    private final InGameParticipantCacheRepository inGameParticipantCacheRepository;

    public void updateLocation(LocationUpdateCommand command) {
        InGameParticipantCache participant = inGameParticipantCacheRepository.findByParticipantId(command.gameId(), command.participantId())
                .orElseThrow(() -> new ApplicationException(GameParticipantException.NOT_A_PARTICIPANT));

        if (participant.team() != Team.ROBBER) {
            throw new ApplicationException(GameParticipantException.NOT_RUNNER_TEAM);
        }

        SystemEventData.RunnerLocation location =
                SystemEventData.RunnerLocation.of(
                        command.participantId(),
                        participant.nickname(),
                        command.latitude(),
                        command.longitude()
                );

        runnerLocationRepository.save(command.gameId(), command.participantId(), location);
    }

    /**
     * 도둑 위치 조회 (주기적 공개 스케줄러 용)
     *   - ALIVE 상태의 도둑 위치만 조회
     *   - 조회 후, 스냅샷 생성
     */
    public List<SystemEventData.RunnerLocation> revealRunnerLocations(Long gameId) {
        Set<Long> aliveRunnerIds = gameParticipantRepository.findAliveRunnerIdsByGameId(gameId);
        List<SystemEventData.RunnerLocation> locations = runnerLocationRepository.findAllByGameId(gameId).stream()
                .filter(loc -> aliveRunnerIds.contains(loc.participantId()))
                .toList();
        runnerLocationSnapshotRepository.saveAll(gameId, locations);
        return locations;
    }

    public void clearRunnerLocations(Long gameId) {
        runnerLocationRepository.deleteAllByGameId(gameId);
    }

    public void clearRunnerLocationSnapshot(Long gameId) {
        runnerLocationSnapshotRepository.deleteAllByGameId(gameId);
    }
}
