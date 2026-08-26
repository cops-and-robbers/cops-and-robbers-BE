package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.exception.CommunityPostReactionException;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityPostScrapListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "CommunityPostReaction", description = "커뮤니티 게시글 좋아요·스크랩 API")
public interface CommunityPostReactionControllerDocs {

    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 누릅니다. 이미 좋아요한 게시글이면 409를 반환합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityPostReactionException.class, codes = {"ALREADY_LIKED"})
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "좋아요 성공")})
    ResponseEntity<Void> likePost(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") Long postId
    );

    @Operation(summary = "게시글 좋아요 취소", description = "게시글 좋아요를 취소합니다. 좋아요한 적이 없으면 404를 반환합니다.")
    @ApiErrorCode(value = CommunityPostReactionException.class, codes = {"LIKE_NOT_FOUND"})
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "좋아요 취소 성공")})
    ResponseEntity<Void> unlikePost(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") Long postId
    );

    @Operation(summary = "게시글 스크랩", description = "게시글을 스크랩합니다. 이미 스크랩한 게시글이면 409를 반환합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityPostReactionException.class, codes = {"ALREADY_SCRAPPED"})
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "스크랩 성공")})
    ResponseEntity<Void> scrapPost(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") Long postId
    );

    @Operation(summary = "게시글 스크랩 취소", description = "게시글 스크랩을 취소합니다. 스크랩한 적이 없으면 404를 반환합니다.")
    @ApiErrorCode(value = CommunityPostReactionException.class, codes = {"SCRAP_NOT_FOUND"})
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "스크랩 취소 성공")})
    ResponseEntity<Void> unscrapPost(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") Long postId
    );

    @Operation(summary = "내 스크랩 목록 조회",
            description = "로그인한 사용자가 스크랩한 게시글 목록을 스크랩한 순서(최신순)로 조회합니다.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "조회 성공")})
    ResponseEntity<CommunityPostScrapListResponse> getMyScraps(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "이전 응답의 nextCursor. 첫 페이지는 생략", example = "12") Long cursor,
            @Parameter(description = "페이지 크기 (1~50)", example = "10") int size
    );
}
