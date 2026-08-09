package com.team.cops_and_robbers.community.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommunityPostException implements ExceptionCode {

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없음", "해당 게시글을 찾을 수 없습니다."),
    FORBIDDEN_NOT_AUTHOR(HttpStatus.FORBIDDEN, "권한 없음", "작성자만 수정, 삭제, 상태 변경할 수 있습니다."),
    INVALID_MEETING_DATE(HttpStatus.BAD_REQUEST, "잘못된 모임 날짜", "모임 날짜는 현재 시간 이후여야 합니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
