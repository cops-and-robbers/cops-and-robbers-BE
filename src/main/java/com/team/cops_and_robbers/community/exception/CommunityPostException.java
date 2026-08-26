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
    INVALID_MEETING_DATE(HttpStatus.BAD_REQUEST, "잘못된 모임 날짜", "모임 날짜는 현재 시간 이후여야 합니다."),
    ADDRESS_NOT_FOUND(HttpStatus.BAD_REQUEST, "주소를 찾을 수 없는 위치", "선택한 위치의 주소를 찾을 수 없습니다. 다른 장소를 선택해주세요."),
    UNSUPPORTED_LIST_SCOPE(HttpStatus.BAD_REQUEST, "지원하지 않는 조회 범위", "현재는 전체 조회(scope=ALL)만 지원합니다."),
    COUNTRY_NOT_SPECIFIED(HttpStatus.BAD_REQUEST, "국가를 특정할 수 없음", "countryCode 또는 현재 위치(latitude, longitude) 중 하나는 필수입니다."),
    ADDRESS_LOOKUP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "주소 조회 실패", "주소를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
