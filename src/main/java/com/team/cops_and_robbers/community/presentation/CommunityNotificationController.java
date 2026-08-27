package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.presentation.annotation.AllowedQueryParams;
import com.team.cops_and_robbers.community.application.CommunityNotificationService;
import com.team.cops_and_robbers.community.application.dto.command.CommunityNotificationListCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationListResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationUnreadCountResult;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityNotificationListResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityNotificationUnreadCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community-posts/notifications")
@RequiredArgsConstructor
public class CommunityNotificationController implements CommunityNotificationControllerDocs {

    private final CommunityNotificationService communityNotificationService;

    @AllowedQueryParams({"cursor", "size"})
    @GetMapping
    public ResponseEntity<CommunityNotificationListResponse> getNotifications(
            @AuthUser LoginUser loginUser,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        CommunityNotificationListResult result = communityNotificationService.getNotifications(
                CommunityNotificationListCommand.of(loginUser.userId(), cursor, size));
        return ResponseEntity.ok(CommunityNotificationListResponse.from(result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<CommunityNotificationUnreadCountResponse> getUnreadCount(
            @AuthUser LoginUser loginUser
    ) {
        CommunityNotificationUnreadCountResult result =
                communityNotificationService.getUnreadCount(loginUser.userId());
        return ResponseEntity.ok(CommunityNotificationUnreadCountResponse.from(result));
    }

    @PostMapping("/read")
    public ResponseEntity<Void> readNotifications(@AuthUser LoginUser loginUser) {
        communityNotificationService.readNotifications(loginUser.userId());
        return ResponseEntity.noContent().build();
    }
}
