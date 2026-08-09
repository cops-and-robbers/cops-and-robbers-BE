package com.team.cops_and_robbers.bug.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BugReportException implements ExceptionCode {

    BUG_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "버그 제보를 찾을 수 없습니다.", "해당 버그 제보가 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
