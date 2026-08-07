package com.team.cops_and_robbers.community.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record CommunityPostListCommand(
        int page,
        int size
) {
    public CommunityPostListCommand {
        if (page < 0) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
        if (size < 1) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
