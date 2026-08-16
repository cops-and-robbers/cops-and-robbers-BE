package com.team.cops_and_robbers.community.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.community.domain.CommunityPostScope;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.exception.CommunityPostException;

public record CommunityPostListCommand(
        String cursor,
        int size,
        CommunityPostScope scope,
        CommunityPostSort sort
) {
    private static final int MAX_SIZE = 100;

    public CommunityPostListCommand {
        if (size < 1 || size > MAX_SIZE) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
        if (scope != CommunityPostScope.ALL) {
            throw new ApplicationException(CommunityPostException.UNSUPPORTED_LIST_SCOPE);
        }
        if (sort != CommunityPostSort.LATEST) {
            throw new ApplicationException(CommunityPostException.UNSUPPORTED_LIST_SORT);
        }
    }
}
