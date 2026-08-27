package com.team.cops_and_robbers.community.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record CommunityNotificationListCommand(
        Long userId,
        Long cursor,
        int size
) {
    private static final int MAX_SIZE = 50;
    private static final int FIRST_PAGE = 0;

    public CommunityNotificationListCommand {
        if (size < 1 || size > MAX_SIZE) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
    }

    public static CommunityNotificationListCommand of(Long userId, Long cursor, int size) {
        return new CommunityNotificationListCommand(userId, cursor, size);
    }

    /** 다음 페이지가 남았는지는 요청 크기보다 한 건 더 조회해서 판단하고, 그 한 건은 응답에서 걷어낸다. */
    public Pageable toPageable() {
        return PageRequest.of(FIRST_PAGE, size + 1);
    }
}
