package com.team.cops_and_robbers.community.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.community.domain.CommunityPostScope;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import org.springframework.util.StringUtils;

/** 목록은 국가 단위로 나눠서 내려준다. 국가 코드는 GET /api/community-posts/country 로 먼저 조회한다. */
public record CommunityPostListCommand(
        String cursor,
        int size,
        CommunityPostScope scope,
        CommunityPostSort sort,
        String countryCode
) {
    private static final int MAX_SIZE = 100;

    public CommunityPostListCommand {
        if (size < 1 || size > MAX_SIZE) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
        if (!StringUtils.hasText(countryCode)) {
            throw new ApplicationException(CommunityPostException.COUNTRY_NOT_SPECIFIED);
        }
        if (scope != CommunityPostScope.ALL) {
            throw new ApplicationException(CommunityPostException.UNSUPPORTED_LIST_SCOPE);
        }
        if (sort != CommunityPostSort.LATEST) {
            throw new ApplicationException(CommunityPostException.UNSUPPORTED_LIST_SORT);
        }
    }
}
