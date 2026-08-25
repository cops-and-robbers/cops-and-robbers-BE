package com.team.cops_and_robbers.community.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommunityPostReactionException implements ExceptionCode {

    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요한 게시글", "이미 좋아요를 누른 게시글입니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요를 찾을 수 없음", "좋아요를 누른 적이 없는 게시글입니다."),
    ALREADY_SCRAPPED(HttpStatus.CONFLICT, "이미 스크랩한 게시글", "이미 스크랩한 게시글입니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "스크랩을 찾을 수 없음", "스크랩한 적이 없는 게시글입니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
