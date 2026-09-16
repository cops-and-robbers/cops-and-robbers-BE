package com.team.cops_and_robbers.play.notification.application;

import com.team.cops_and_robbers.common.fcm.FcmMessage;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.chat.domain.ChatMessage;
import com.team.cops_and_robbers.play.chat.domain.ChatScope;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameFcmNotifier {

    private static final String CHAT_PUSH_TYPE = "CHAT";
    private static final String CHAT_COLLAPSE_KEY_PREFIX = "chat:";
    private static final String CHAT_PUSH_TITLE = "새 채팅";
    private static final String CHAT_PUSH_BODY = "새 채팅이 도착했습니다.";

    private final FcmService fcmService;
    private final InGameParticipantCacheRepository inGameParticipantCacheRepository;
    private final ChatPushDebouncer chatPushDebouncer;

    @Async("fcmExecutor")
    public CompletableFuture<Void> notifySystemEvent(SystemEvent event) {
        try {
            List<String> tokens = getGameTokens(event.gameId());
            if (tokens.isEmpty()) {
                log.warn("[FCM] No tokens found | gameId={}, type={}", event.gameId(), event.type());
                return CompletableFuture.completedFuture(null);
            }

            FcmPayload payload = resolveSystemPayload(event);
            fcmService.send(new FcmMessage(tokens, payload.title(), payload.body(), payload.data()));
        } catch (Exception e) {
            log.error("[FCM] Async send failed | gameId={}, type={}", event.gameId(), event.type(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private List<String> getGameTokens(Long gameId) {
        return inGameParticipantCacheRepository.findAllByGameId(gameId).stream()
                .map(InGameParticipantCache::fcmToken)
                .filter(Objects::nonNull)
                .toList();
    }

    private FcmPayload resolveSystemPayload(SystemEvent event) {
        SystemEventType type = event.type();
        Map<String, String> data = Map.of("type", type.name(), "gameId", String.valueOf(event.gameId()));
        return switch (type) {
            case ARREST -> new FcmPayload("도둑 체포!", "도둑이 체포되었습니다.", data);
            case ESCAPE -> new FcmPayload("도둑 탈옥!", "도둑이 감옥에서 탈옥했습니다!", data);
            case GAME_OVER -> new FcmPayload("게임 종료", "게임이 종료되었습니다. 결과를 확인하세요!", data);
            case ROBBER_LOCATION_REVEAL -> new FcmPayload("도둑 위치 공개!", "도둑의 현재 위치가 공개되었습니다!", data);
            case POLICE_MOVE_START -> new FcmPayload("경찰 이동 시작!", "경찰이 이동을 시작했습니다!", data);
            case PLAYER_LEFT -> {
                SystemEventData.PlayerLeftData playerLeft = (SystemEventData.PlayerLeftData) event.data();
                yield new FcmPayload(playerLeft.team().getDisplayName() + " 참가자 퇴장", playerLeft.nickname() + "님이 게임에서 퇴장했습니다.", data);
            }
        };
    }

    @Async("fcmExecutor")
    public CompletableFuture<Void> notifyChatMessage(ChatMessage message) {
        try {
            // 창이 열려 있으면 캐시 조회·팬아웃을 스킵한다
            if (!chatPushDebouncer.tryOpenWindow(message.gameId())) {
                return CompletableFuture.completedFuture(null);
            }

            List<String> tokens = getChatTokens(message);
            if (tokens.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            FcmPayload payload = resolveChatPayload(message);
            fcmService.send(new FcmMessage(tokens, payload.title(), payload.body(), payload.data(),
                    CHAT_COLLAPSE_KEY_PREFIX + message.gameId()));
        } catch (Exception e) {
            log.error("[FCM] Async chat send failed | gameId={}, participantId={}", message.gameId(), message.sender().participantId(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private List<String> getChatTokens(ChatMessage message) {
        Long senderId = message.sender().participantId();
        Team targetTeam = message.scope() == ChatScope.TEAM ? message.sender().team() : null;

        return inGameParticipantCacheRepository.findAllEntriesByGameId(message.gameId()).entrySet().stream()
                .filter(e -> !e.getKey().equals(senderId))
                .filter(e -> targetTeam == null || e.getValue().team() == targetTeam)
                .filter(e -> e.getValue().fcmToken() != null)
                .map(e -> e.getValue().fcmToken())
                .toList();
    }

    // 방 단위로 묶여 나가므로 고정 문구를 쓴다.
    private FcmPayload resolveChatPayload(ChatMessage message) {
        Map<String, String> data = Map.of("type", CHAT_PUSH_TYPE, "gameId", String.valueOf(message.gameId()), "scope", message.scope().name());
        return new FcmPayload(CHAT_PUSH_TITLE, CHAT_PUSH_BODY, data);
    }

    private record FcmPayload(String title, String body, Map<String, String> data) {}
}
