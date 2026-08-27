package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.exception.CommunityCommentException;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityCommentCreateRequest;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityCommentNotificationRequest;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityCommentListResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityCommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "CommunityComment", description = "커뮤니티 게시글 댓글 API")
public interface CommunityCommentControllerDocs {

    @Operation(summary = "댓글 목록 조회",
            description = """
                    게시글의 댓글을 오래된 순으로 조회합니다. 로그인 없이 조회할 수 있습니다.

                    커서 페이징은 1depth 댓글에만 적용되고, 그 페이지에 속한 답글은 각 댓글의 `replies`에 전부 담깁니다.
                    `deleted`가 `true`인 댓글은 답글이 남아 자리만 지키고 있는 것으로, `content`와 작성자가 `null`입니다.
                    클라이언트에서 '삭제된 댓글입니다'로 표시해주세요.
                    """)
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "조회 성공")})
    ResponseEntity<CommunityCommentListResponse> getComments(
            @Parameter(description = "게시글 ID", example = "1") Long postId,
            @Parameter(description = "이전 응답의 nextCursor. 첫 페이지는 생략", example = "12") Long cursor,
            @Parameter(description = "페이지 크기 (1~50)", example = "20") int size
    );

    @Operation(summary = "댓글 작성",
            description = """
                    댓글을 작성합니다. `parentId`를 함께 보내면 그 댓글의 답글이 됩니다.

                    댓글은 2depth까지만 허용해서, 답글에 다시 답글을 달 수 없습니다.
                    이미 답글인 댓글의 ID를 `parentId`로 보내면 `INVALID_COMMENT_DEPTH`로 거절합니다.
                    """)
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityCommentException.class,
            codes = {"PARENT_COMMENT_NOT_FOUND", "INVALID_COMMENT_DEPTH",
                    "PARENT_COMMENT_POST_MISMATCH", "DELETED_COMMENT_CANNOT_REPLY"})
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "작성 성공")})
    ResponseEntity<CommunityCommentResponse> createComment(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") Long postId,
            CommunityCommentCreateRequest request
    );

    @Operation(summary = "댓글 알림 켜기/끄기",
            description = """
                    내가 쓴 댓글에 답글이 달렸을 때 알림을 받을지 정합니다. 기본값은 받음입니다.

                    게시글 알림 설정과 독립입니다. 그 글의 알림을 꺼도 내 댓글의 답글 알림은 이 값만 따릅니다.
                    현재 상태는 댓글 목록 응답의 `notifyReplies`로 내려갑니다.
                    """)
    @ApiErrorCode(value = CommunityCommentException.class,
            codes = {"COMMENT_NOT_FOUND", "FORBIDDEN_NOT_COMMENT_AUTHOR"})
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "설정 성공")})
    ResponseEntity<Void> updateCommentNotification(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "댓글 ID", example = "1") Long commentId,
            CommunityCommentNotificationRequest request
    );

    @Operation(summary = "댓글 삭제",
            description = """
                    작성자만 삭제할 수 있습니다.

                    답글이 남아 있는 댓글은 목록에서 완전히 사라지지 않고 `deleted: true`로 남습니다.
                    남의 답글이 매달릴 자리를 없앨 수 없기 때문입니다. 마지막 답글까지 지워지면 그때 함께 사라집니다.
                    """)
    @ApiErrorCode(value = CommunityCommentException.class,
            codes = {"COMMENT_NOT_FOUND", "FORBIDDEN_NOT_COMMENT_AUTHOR"})
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "삭제 성공")})
    ResponseEntity<Void> deleteComment(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "댓글 ID", example = "1") Long commentId
    );
}
