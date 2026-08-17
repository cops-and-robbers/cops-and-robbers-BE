package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.presentation.annotation.AllowedQueryParams;
import com.team.cops_and_robbers.community.application.AddressService;
import com.team.cops_and_robbers.community.application.CommunityPostService;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostDeleteCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostListCommand;
import com.team.cops_and_robbers.community.application.dto.result.AddressResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostCursorResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.domain.CommunityPostScope;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostCreateRequest;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostStatusRequest;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostUpdateRequest;
import com.team.cops_and_robbers.community.presentation.dto.response.AddressResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityPostListResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityPostResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community-posts")
@RequiredArgsConstructor
public class CommunityPostController implements CommunityPostControllerDocs {

    private final CommunityPostService communityPostService;
    private final AddressService addressService;

    @AllowedQueryParams({"latitude", "longitude"})
    @GetMapping("/address")
    public ResponseEntity<AddressResponse> getAddress(
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        AddressResult result = addressService.getAddress(latitude, longitude);
        AddressResponse response = AddressResponse.from(result);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CommunityPostResponse> createPost(
            @AuthUser LoginUser loginUser,
            @RequestBody @Valid CommunityPostCreateRequest request
    ) {
        CommunityPostResult result = communityPostService.createPost(request.toCommand(loginUser.userId()));
        CommunityPostResponse response = CommunityPostResponse.from(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @AllowedQueryParams({"cursor", "size", "scope", "sort"})
    @GetMapping
    public ResponseEntity<CommunityPostListResponse> getPostList(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") CommunityPostScope scope,
            @RequestParam(defaultValue = "LATEST") CommunityPostSort sort
    ) {
        CommunityPostListCommand command = new CommunityPostListCommand(cursor, size, scope, sort);
        CommunityPostCursorResult result = communityPostService.getPostList(command);
        CommunityPostListResponse response = CommunityPostListResponse.from(result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<CommunityPostResponse> getPost(
            @PathVariable Long postId
    ) {
        CommunityPostResult result = communityPostService.getPost(postId);
        CommunityPostResponse response = CommunityPostResponse.from(result);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<CommunityPostResponse> updatePost(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityPostUpdateRequest request
    ) {
        CommunityPostResult result = communityPostService.updatePost(request.toCommand(loginUser.userId(), postId));
        CommunityPostResponse response = CommunityPostResponse.from(result);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        communityPostService.deletePost(new CommunityPostDeleteCommand(loginUser.userId(), postId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{postId}/status")
    public ResponseEntity<CommunityPostResponse> updateStatus(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityPostStatusRequest request
    ) {
        CommunityPostResult result = communityPostService.updateStatus(request.toCommand(loginUser.userId(), postId));
        CommunityPostResponse response = CommunityPostResponse.from(result);
        return ResponseEntity.ok(response);
    }
}
