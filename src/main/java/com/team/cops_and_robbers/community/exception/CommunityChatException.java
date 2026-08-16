package com.team.cops_and_robbers.community.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommunityChatException implements ExceptionCode {

    NOT_A_CHAT_MEMBER(HttpStatus.FORBIDDEN, "채팅방 참여자 아님", "채팅방에 참여한 사용자만 이용할 수 있습니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여함", "이미 참여한 채팅방입니다."),
    CHAT_ROOM_FULL(HttpStatus.BAD_REQUEST, "정원 초과", "모집 정원이 가득 찼습니다."),
    RECRUITMENT_CLOSED(HttpStatus.BAD_REQUEST, "모집 종료", "모집이 종료되어 참여할 수 없습니다."),
    AUTHOR_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "작성자 나가기 불가", "작성자는 채팅방을 나갈 수 없습니다. 게시글을 삭제해주세요."),
    INVALID_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "잘못된 메시지 타입", "전송할 수 없는 메시지 타입입니다."),
    EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "빈 메시지", "메시지 내용을 입력해주세요."),
    MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지 길이 초과", "메시지는 500자 이하로 입력해주세요.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}