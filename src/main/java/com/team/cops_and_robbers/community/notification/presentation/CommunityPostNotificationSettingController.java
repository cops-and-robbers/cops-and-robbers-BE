package com.team.cops_and_robbers.community.notification.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.community.notification.application.CommunityNotificationService;
import com.team.cops_and_robbers.community.notification.presentation.dto.request.CommunityPostNotificationSettingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community-posts")
@RequiredArgsConstructor
public class CommunityPostNotificationSettingController implements CommunityPostNotificationSettingControllerDocs {

    private final CommunityNotificationService communityNotificationService;

    @PutMapping("/{postId}/notification-settings")
    public ResponseEntity<Void> updateNotificationSetting(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityPostNotificationSettingRequest request
    ) {
        communityNotificationService.updateSetting(request.toCommand(loginUser.userId(), postId));
        return ResponseEntity.noContent().build();
    }
}
