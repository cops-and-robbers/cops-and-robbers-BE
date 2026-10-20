package com.team.cops_and_robbers.play.distance.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.history.application.dto.ParticipantDistance;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.distance.application.dto.command.DistanceUpdateCommand;
import com.team.cops_and_robbers.play.distance.domain.ParticipantDistanceHash;
import com.team.cops_and_robbers.play.distance.repository.ParticipantDistanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantDistanceService {

    private static final double MAX_SPEED_METERS_PER_SECOND = 25_000.0 / 3_600.0;

    private final Clock clock;
    private final InGameParticipantCacheRepository inGameParticipantCacheRepository;
    private final ParticipantDistanceRepository participantDistanceRepository;

    public void updateDistance(DistanceUpdateCommand command) {
        inGameParticipantCacheRepository.findByParticipantId(command.gameId(), command.participantId())
                .orElseThrow(() -> new ApplicationException(GameParticipantException.NOT_A_PARTICIPANT));

        long now = clock.millis();
        ParticipantDistanceHash previous = participantDistanceRepository
                .findByParticipantId(command.gameId(), command.participantId())
                .orElse(null);

        if (isAbnormal(command.distanceMeters(), previous, now)) {
            log.debug("DISTANCE_IGNORED gameId={} participantId={} distanceMeters={}",
                    command.gameId(), command.participantId(), command.distanceMeters());
            return;
        }

        participantDistanceRepository.save(
                command.gameId(),
                command.participantId(),
                new ParticipantDistanceHash(command.distanceMeters(), now)
        );
    }

    public List<ParticipantDistance> getDistances(Long gameId) {
        return participantDistanceRepository.findAllByGameId(gameId).entrySet().stream()
                .map(entry -> new ParticipantDistance(entry.getKey(), entry.getValue().distanceMeters()))
                .toList();
    }

    public void clearDistances(Long gameId) {
        participantDistanceRepository.deleteAllByGameId(gameId);
    }

    private boolean isAbnormal(Double distanceMeters, ParticipantDistanceHash previous, long now) {
        if (distanceMeters == null || !Double.isFinite(distanceMeters) || distanceMeters < 0) {
            return true;
        }
        if (previous == null) {
            return false;
        }

        double delta = distanceMeters - previous.distanceMeters();
        if (delta < 0) {
            return true;
        }

        double elapsedSeconds = (now - previous.updatedAtEpochMilli()) / 1_000.0;
        return delta > MAX_SPEED_METERS_PER_SECOND * elapsedSeconds;
    }
}
