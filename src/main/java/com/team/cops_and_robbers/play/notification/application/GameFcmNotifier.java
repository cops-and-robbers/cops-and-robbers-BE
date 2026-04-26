package com.team.cops_and_robbers.play.notification.application;

import com.team.cops_and_robbers.common.fcm.FcmMessage;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.notification.repository.FcmTokenCacheRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameFcmNotifier {

    private final FcmService fcmService;
    private final GameParticipantRepository gameParticipantRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final FcmTokenCacheRepository fcmTokenCacheRepository;

    @Async("fcmExecutor")
    public void notifySystemEvent(SystemEvent event) {
        try {
            if (event.type() == SystemEventType.GAME_OVER) {
                fcmTokenCacheRepository.deleteAllByGameId(event.gameId());
            }

            List<String> tokens = getGameTokens(event.gameId());
            if (tokens.isEmpty()) return;

            FcmPayload payload = resolveSystemPayload(event.type(), event.gameId());
            fcmService.send(new FcmMessage(tokens, payload.title(), payload.body(), payload.data()));
        } catch (Exception e) {
            log.error("[FCM] Async send failed | gameId={}, type={}", event.gameId(), event.type(), e);
        }
    }

    private List<String> getGameTokens(Long gameId) {
        List<String> cached = fcmTokenCacheRepository.findGameTokens(gameId);
        if (cached != null) return cached;

        List<Long> userIds = gameParticipantRepository.findUserIdsByGameId(gameId);
        List<String> tokens = userIds.isEmpty()
                ? List.of()
                : userDeviceRepository.findFcmTokensByUserIdsAndAllowPush(userIds);
        fcmTokenCacheRepository.saveGameTokens(gameId, tokens);
        return tokens;
    }

    private FcmPayload resolveSystemPayload(SystemEventType type, Long gameId) {
        Map<String, String> data = Map.of("type", type.name(), "gameId", String.valueOf(gameId));
        return switch (type) {
            case ARREST -> new FcmPayload("🚔 도둑 체포!", "도둑이 체포되었습니다.", data);
            case ESCAPE -> new FcmPayload("🏃 도둑 탈옥!", "도둑이 감옥에서 탈옥했습니다!", data);
            case GAME_OVER -> new FcmPayload("🏁 게임 종료", "게임이 종료되었습니다. 결과를 확인하세요!", data);
            case ROBBER_LOCATION_REVEAL -> new FcmPayload("📍 도둑 위치 공개!", "도둑의 현재 위치가 공개되었습니다!", data);
            case POLICE_MOVE_START -> new FcmPayload("🚨 경찰 이동 시작!", "경찰이 이동을 시작했습니다!", data);
        };
    }

    private record FcmPayload(String title, String body, Map<String, String> data) {}
}
