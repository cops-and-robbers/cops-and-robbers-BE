package com.team.cops_and_robbers.community.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.community.domain.CommunityPostScope;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import org.springframework.util.StringUtils;

/**
 * 목록은 국가 단위로 나눠서 내려준다. 나라를 특정하는 방법이 두 가지다.
 * <p>
 * 이미 국가를 아는 클라이언트는 countryCode를, 모르는 클라이언트(웹 첫 진입 등)는 현재 좌표를 보낸다.
 * 좌표로 오면 서버가 역지오코딩해 국가를 판별하므로 매 페이지마다 보내면 벤더 호출이 그만큼 늘어난다.
 * 첫 페이지만 좌표로 받고 응답의 countryCode를 다음 페이지부터 되돌려주는 것을 전제로 한다.
 */
public record CommunityPostListCommand(
        String cursor,
        int size,
        CommunityPostScope scope,
        CommunityPostSort sort,
        String countryCode,
        Double latitude,
        Double longitude
) {
    private static final int MAX_SIZE = 100;

    public CommunityPostListCommand {
        if (size < 1 || size > MAX_SIZE) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
        if (!hasCountryCode(countryCode) && !hasCoordinates(latitude, longitude)) {
            throw new ApplicationException(CommunityPostException.COUNTRY_NOT_SPECIFIED);
        }
        if (scope != CommunityPostScope.ALL) {
            throw new ApplicationException(CommunityPostException.UNSUPPORTED_LIST_SCOPE);
        }
        if (sort != CommunityPostSort.LATEST) {
            throw new ApplicationException(CommunityPostException.UNSUPPORTED_LIST_SORT);
        }
    }

    public boolean needsCountryLookup() {
        return !hasCountryCode(countryCode);
    }

    private static boolean hasCountryCode(String countryCode) {
        return StringUtils.hasText(countryCode);
    }

    private static boolean hasCoordinates(Double latitude, Double longitude) {
        return latitude != null && longitude != null;
    }
}
