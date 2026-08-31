package com.team.cops_and_robbers.community.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommunityCommentException implements ExceptionCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없음", "해당 댓글을 찾을 수 없습니다."),
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없음", "답글을 달 댓글을 찾을 수 없습니다."),
    FORBIDDEN_NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "권한 없음", "작성자만 변경하거나 삭제할 수 있습니다."),
    INVALID_COMMENT_DEPTH(HttpStatus.BAD_REQUEST, "답글에 답글", "답글에는 답글을 달 수 없습니다."),
    PARENT_COMMENT_POST_MISMATCH(HttpStatus.BAD_REQUEST, "다른 게시글의 댓글", "다른 게시글의 댓글에는 답글을 달 수 없습니다."),
    DELETED_COMMENT_CANNOT_REPLY(HttpStatus.BAD_REQUEST, "삭제된 댓글", "삭제된 댓글에는 답글을 달 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
