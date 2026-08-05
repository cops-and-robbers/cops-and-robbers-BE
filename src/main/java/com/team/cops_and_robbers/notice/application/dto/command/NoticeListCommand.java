package com.team.cops_and_robbers.notice.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record NoticeListCommand(
        int page,
        int size,
        NoticeCategory category
) {
    public NoticeListCommand {
        if (page < 0) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
        if (size < 1) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
    }

    public static NoticeListCommand of(int page, int size) {
        return new NoticeListCommand(page, size, null);
    }

    public static NoticeListCommand of(int page, int size, NoticeCategory category) {
        return new NoticeListCommand(page, size, category);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
