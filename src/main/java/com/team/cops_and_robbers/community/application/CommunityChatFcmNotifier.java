package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.fcm.FcmMessage;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.community.application.event.CommunityChatPinChangedEvent;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityChatPin;
import com.team.cops_and_robbers.community.domain.CommunityChatSystemEventType;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.user.domain.UserDevice;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
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
public class CommunityChatFcmNotifier {

    private static final String GAME_INVITE_BODY = "게임에 초대했습니다";
    private static final String PIN_PUSH_TITLE = "고정 채팅";
    private static final Map<CommunityChatSystemEventType, String> PIN_PUSH_BODIES = Map.of(
            CommunityChatSystemEventType.PIN_REGISTERED, "방장님이 고정 채팅을 등록했습니다",
            CommunityChatSystemEventType.PIN_UPDATED, "방장님이 고정 채팅을 수정했습니다",
            CommunityChatSystemEventType.PIN_DELETED, "방장님이 고정 채팅을 삭제했습니다"
    );

    private final FcmService fcmService;
    private final CommunityChatMemberRepository communityChatMemberRepository;
    private final UserDeviceRepository userDeviceRepository;

    /**
     * 지금 그 방을 보고 있는 사람은 서버가 알 수 없어 걸러내지 않는다.
     * 앱이 포그라운드에서 현재 방이면 표시하지 않는 쪽으로 처리한다.
     */
    @Async("fcmExecutor")
    public CompletableFuture<Void> notifyMessageSent(CommunityChatMessage message) {
        try {
            List<String> tokens = getChatTokens(message);
            if (tokens.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            FcmPayload payload = resolveChatPayload(message);
            fcmService.send(new FcmMessage(tokens, payload.title(), payload.body(), payload.data()));
        } catch (Exception e) {
            log.error("[FCM] Async send failed | postId={}, messageId={}",
                    message.getCommunityPostId(), message.getId(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("fcmExecutor")
    public CompletableFuture<Void> notifyPinChanged(CommunityChatPinChangedEvent event) {
        try {
            CommunityChatPin pin = event.pin();
            List<String> tokens = resolvePushTargets(pin.getCommunityPostId(), event.actorId());
            if (tokens.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            Map<String, String> data = Map.of(
                    "type", event.action().name(),
                    "postId", String.valueOf(pin.getCommunityPostId())
            );
            String body = PIN_PUSH_BODIES.get(event.action());
            fcmService.send(new FcmMessage(tokens, PIN_PUSH_TITLE, body, data));
        } catch (Exception e) {
            log.error("[FCM] Async pin push failed | postId={}", event.pin().getCommunityPostId(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private List<String> getChatTokens(CommunityChatMessage message) {
        if (message.getMessageType() == CommunityChatMessageType.SYSTEM) {
            return List.of();
        }
        return resolvePushTargets(message.getCommunityPostId(), message.getSenderId());
    }

    private List<String> resolvePushTargets(Long postId, Long excludeUserId) {
        List<Long> userIds = communityChatMemberRepository.findPushTargetUserIds(postId, excludeUserId);
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userDeviceRepository.findByUser_IdIn(userIds).stream()
                .filter(device -> device.getUser().isAllowCommunityPush())
                .map(UserDevice::getFcmToken)
                .filter(Objects::nonNull)
                .toList();
    }

    private FcmPayload resolveChatPayload(CommunityChatMessage message) {
        CommunityChatMessageType type = message.getMessageType();
        Map<String, String> data = Map.of(
                "type", type.name(),
                "postId", String.valueOf(message.getCommunityPostId()));
        String body = type == CommunityChatMessageType.GAME_INVITE ? GAME_INVITE_BODY : message.getMessage();
        return new FcmPayload(message.getSenderNickname(), body, data);
    }

    private record FcmPayload(String title, String body, Map<String, String> data) {}
}
