package com.team.cops_and_robbers.play.common.application;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserFcmTokenProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InGameParticipantCacheService {

    private final GameParticipantRepository gameParticipantRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final InGameParticipantCacheRepository inGameParticipantCacheRepository;

    @Transactional(readOnly = true)
    public void loadCache(Long gameId) {
        List<GameParticipant> participants = gameParticipantRepository.findAllByGameIdWithUser(gameId);
        Map<Long, String> fcmTokenByUserId = fetchFcmTokenByUserId(participants);
        inGameParticipantCacheRepository.saveAll(gameId, participants, fcmTokenByUserId);
    }

    private Map<Long, String> fetchFcmTokenByUserId(List<GameParticipant> participants) {
        List<Long> userIds = participants.stream()
                .map(p -> p.getUser().getId())
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userDeviceRepository.findFcmTokenProjectionsByUserIdsAndAllowGamePush(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserFcmTokenProjection::getUserId,
                        UserFcmTokenProjection::getFcmToken
                ));
    }

    public void clearCache(Long gameId) {
        inGameParticipantCacheRepository.deleteAllByGameId(gameId);
    }
}
