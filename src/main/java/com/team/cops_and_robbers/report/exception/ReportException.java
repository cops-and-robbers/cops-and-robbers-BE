package com.team.cops_and_robbers.report.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportException implements ExceptionCode {

    ETC_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "기타 사유를 입력해주세요.", "신고 유형이 기타일 때 사유를 입력해야 합니다."),
    SELF_REPORT(HttpStatus.BAD_REQUEST, "본인을 신고할 수 없습니다.", "본인을 신고할 수 없습니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "이미 신고한 사용자입니다.", "해당 게임에서 이미 신고한 사용자입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다.", "해당 신고 내역이 존재하지 않습니다."),
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다.", "해당 게임에 존재하지 않는 참가자입니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

}
