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
    JOINED_CHAT_ROOM_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "참여 채팅방 수 초과", "참여할 수 있는 채팅방은 최대 100개입니다. 기존 채팅방을 나간 후 다시 시도해주세요."),
    RECRUITMENT_CLOSED(HttpStatus.BAD_REQUEST, "모집 종료", "모집이 종료되어 참여할 수 없습니다."),
    AUTHOR_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "작성자 나가기 불가", "작성자는 채팅방을 나갈 수 없습니다. 게시글을 삭제해주세요."),
    FORBIDDEN_NOT_CHAT_HOST(HttpStatus.FORBIDDEN, "방장 아님", "방장만 멤버를 강퇴할 수 있습니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "자기 자신 강퇴 불가", "본인은 강퇴할 수 없습니다."),
    CHAT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방 멤버를 찾을 수 없음", "해당 유저는 이 채팅방 멤버가 아닙니다."),
    INVALID_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "잘못된 메시지 타입", "전송할 수 없는 메시지 타입입니다."),
    INVALID_GAME_INVITE(HttpStatus.BAD_REQUEST, "잘못된 게임 초대", "게임 초대 정보의 형식이 올바르지 않습니다."),
    INVALID_MESSAGE_KEY(HttpStatus.BAD_REQUEST, "잘못된 메시지 키", "메시지 키는 36자 이하로 입력해주세요."),
    EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "빈 메시지", "메시지 내용을 입력해주세요."),
    MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지 길이 초과", "메시지는 500자 이하로 입력해주세요."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅 메시지를 찾을 수 없음", "해당 채팅 메시지를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
