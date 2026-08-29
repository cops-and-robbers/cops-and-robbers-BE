package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.presentation.annotation.AllowedQueryParams;
import com.team.cops_and_robbers.community.application.CommunityPostReactionService;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostScrapListCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostScrapListResult;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityPostScrapListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
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
@RequestMapping("/api/community-posts")
@RequiredArgsConstructor
public class CommunityPostReactionController implements CommunityPostReactionControllerDocs {

    private final CommunityPostReactionService communityPostReactionService;

    @PostMapping("/{postId}/likes")
    public ResponseEntity<Void> likePost(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        communityPostReactionService.likePost(postId, loginUser.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<Void> unlikePost(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        communityPostReactionService.unlikePost(postId, loginUser.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/scraps")
    public ResponseEntity<Void> scrapPost(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        communityPostReactionService.scrapPost(postId, loginUser.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{postId}/scraps")
    public ResponseEntity<Void> unscrapPost(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        communityPostReactionService.unscrapPost(postId, loginUser.userId());
        return ResponseEntity.noContent().build();
    }

    @AllowedQueryParams({"cursor", "size"})
    @GetMapping("/scraps")
    public ResponseEntity<CommunityPostScrapListResponse> getMyScraps(
            @AuthUser LoginUser loginUser,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        CommunityPostScrapListResult result = communityPostReactionService.getMyScraps(
                CommunityPostScrapListCommand.of(loginUser.userId(), cursor, size));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(CommunityPostScrapListResponse.from(result));
    }
}
