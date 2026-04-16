package com.team.cops_and_robbers.play.common.application;

import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InGameParticipantCacheService {

    private final GameParticipantRepository gameParticipantRepository;
    private final InGameParticipantCacheRepository inGameParticipantCacheRepository;

    @Transactional(readOnly = true)
    public void loadCache(Long gameId) {
        inGameParticipantCacheRepository.saveAll(
                gameId,
                gameParticipantRepository.findAllByGameIdWithUser(gameId)
        );
    }

    public void clearCache(Long gameId) {
        inGameParticipantCacheRepository.deleteAllByGameId(gameId);
    }
}
