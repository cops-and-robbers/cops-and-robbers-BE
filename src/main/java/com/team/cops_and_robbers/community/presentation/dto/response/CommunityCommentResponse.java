package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityCommentResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CommunityCommentResponse(
        @Schema(description = "댓글 ID", example = "1")
        Long id,
        @Schema(description = "부모 댓글 ID. 1depth 댓글이면 null", example = "null", nullable = true)
        Long parentId,
        @Schema(description = "작성자 ID (삭제된 댓글이면 null)", example = "7", nullable = true)
        Long writerId,
        @Schema(description = "작성자 닉네임 (삭제된 댓글이면 null, 탈퇴한 유저면 \"알수없음\")",
                example = "무서운경찰관", nullable = true)
        String writerNickname,
        @Schema(description = "작성자 프로필 아이콘 번호 (삭제된 댓글이면 null, 탈퇴한 유저면 기본 아이콘 번호)",
                example = "1", nullable = true)
        Integer writerProfileIcon,
        @Schema(description = "댓글 내용 (삭제된 댓글이면 null)", example = "몇 시에 만나나요?", nullable = true)
        String content,
        @Schema(description = "삭제 여부. true면 답글이 남아 자리만 지킨 댓글이라 "
                + "'삭제된 댓글입니다'로 표시한다", example = "false")
        boolean deleted,
        @Schema(description = "작성일시", example = "2026-08-22T12:00:00+09:00")
        String createdAt,
        @Schema(description = "수정일시", example = "2026-08-22T12:00:00+09:00")
        String updatedAt,
        @Schema(description = "답글 목록. 답글에는 답글을 달 수 없어 항상 비어 있다")
        List<CommunityCommentResponse> replies
) {
    public static CommunityCommentResponse from(CommunityCommentResult result) {
        return new CommunityCommentResponse(
                result.id(),
                result.parentId(),
                result.writerId(),
                result.writerNickname(),
                result.writerProfileIcon(),
                result.content(),
                result.deleted(),
                result.createdAt(),
                result.updatedAt(),
                result.replies().stream().map(CommunityCommentResponse::from).toList()
        );
    }
}
