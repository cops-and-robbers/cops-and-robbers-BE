package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostCreateRequest;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostStatusRequest;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostUpdateRequest;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityPostListResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "CommunityPost", description = "커뮤니티 모집 게시글 API")
public interface CommunityPostControllerDocs {

    @Operation(summary = "게시글 생성", description = "새로운 모집 게시글을 생성합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"INVALID_MEETING_DATE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공")
    })
    ResponseEntity<CommunityPostResponse> createPost(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid CommunityPostCreateRequest request
    );

    @Operation(summary = "게시글 목록 조회", description = "모집 게시글 목록을 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<CommunityPostListResponse> getPostList(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1부터~)", example = "10") @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "게시글 단건 조회", description = "특정 모집 게시글을 조회합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<CommunityPostResponse> getPost(
            @PathVariable Long postId
    );

    @Operation(summary = "게시글 수정", description = "특정 모집 게시글을 수정합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND", "FORBIDDEN_NOT_AUTHOR", "INVALID_MEETING_DATE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공")
    })
    ResponseEntity<CommunityPostResponse> updatePost(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityPostUpdateRequest request
    );

    @Operation(summary = "게시글 삭제", description = "특정 모집 게시글을 삭제합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND", "FORBIDDEN_NOT_AUTHOR"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공 (응답 본문 없음)")
    })
    ResponseEntity<Void> deletePost(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long postId
    );

    @Operation(summary = "모집 상태 변경", description = "모집 게시글의 모집 상태를 변경합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND", "FORBIDDEN_NOT_AUTHOR"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 변경 성공")
    })
    ResponseEntity<CommunityPostResponse> updateStatus(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityPostStatusRequest request
    );
}
