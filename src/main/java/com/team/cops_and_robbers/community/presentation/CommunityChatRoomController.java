package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.community.application.CommunityChatMemberService;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityChatRoomListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community-posts/chat/rooms")
@RequiredArgsConstructor
public class CommunityChatRoomController implements CommunityChatRoomControllerDocs {

    private final CommunityChatMemberService communityChatMemberService;

    @GetMapping
    public ResponseEntity<CommunityChatRoomListResponse> getChatRooms(@AuthUser LoginUser loginUser) {
        CommunityChatRoomListResponse response =
                CommunityChatRoomListResponse.from(communityChatMemberService.getChatRooms(loginUser.userId()));
        return ResponseEntity.ok(response);
    }
}
