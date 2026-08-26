package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.presentation.annotation.AllowedQueryParams;
import com.team.cops_and_robbers.community.application.CommunityCommentService;
import com.team.cops_and_robbers.community.application.dto.command.CommunityCommentDeleteCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityCommentListCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityCommentListResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityCommentResult;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityCommentCreateRequest;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityCommentListResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityCommentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community-posts")
@RequiredArgsConstructor
public class CommunityCommentController implements CommunityCommentControllerDocs {

    private final CommunityCommentService communityCommentService;

    @AllowedQueryParams({"cursor", "size"})
    @GetMapping("/{postId}/comments")
    public ResponseEntity<CommunityCommentListResponse> getComments(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        CommunityCommentListResult result =
                communityCommentService.getComments(CommunityCommentListCommand.of(postId, cursor, size));
        return ResponseEntity.ok(CommunityCommentListResponse.from(result));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommunityCommentResponse> createComment(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityCommentCreateRequest request
    ) {
        CommunityCommentResult result =
                communityCommentService.createComment(request.toCommand(loginUser.userId(), postId));
        return ResponseEntity.status(HttpStatus.CREATED).body(CommunityCommentResponse.from(result));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthUser LoginUser loginUser,
            @PathVariable Long commentId
    ) {
        communityCommentService.deleteComment(
                CommunityCommentDeleteCommand.of(loginUser.userId(), commentId));
        return ResponseEntity.noContent().build();
    }
}
