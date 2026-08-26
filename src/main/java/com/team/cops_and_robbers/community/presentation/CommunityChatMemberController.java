package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.community.application.CommunityChatMemberService;
import com.team.cops_and_robbers.community.application.CommunityChatService;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatHistoryCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatJoinCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatLeaveCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityChatMemberListResult;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityChatHistoryResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityChatMemberListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community-posts/{postId}/chat")
@RequiredArgsConstructor
public class CommunityChatMemberController implements CommunityChatMemberControllerDocs {

    private final CommunityChatMemberService communityChatMemberService;
    private final CommunityChatService communityChatService;

    @PostMapping("/join")
    public ResponseEntity<Void> join(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        communityChatMemberService.join(CommunityChatJoinCommand.of(loginUser.userId(), postId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/leave")
    public ResponseEntity<Void> leave(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        communityChatMemberService.leave(CommunityChatLeaveCommand.of(loginUser.userId(), postId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members")
    public ResponseEntity<CommunityChatMemberListResponse> getMembers(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        CommunityChatMemberListResult result = communityChatMemberService.getMembers(postId, loginUser.userId());
        return ResponseEntity.ok(CommunityChatMemberListResponse.from(result));
    }

    @GetMapping("/messages")
    public ResponseEntity<CommunityChatHistoryResponse> getChatHistory(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        CommunityChatHistoryCommand command =
                CommunityChatHistoryCommand.of(loginUser.userId(), postId, cursor, size);
        CommunityChatHistoryResponse response =
                CommunityChatHistoryResponse.from(communityChatService.getHistory(command));
        return ResponseEntity.ok(response);
    }
}
