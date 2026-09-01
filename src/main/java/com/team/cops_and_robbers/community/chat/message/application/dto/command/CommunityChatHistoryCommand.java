package com.team.cops_and_robbers.community.chat.message.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record CommunityChatHistoryCommand(
        Long userId,
        Long postId,
        Long cursor,
        int size
) {
    private static final int MAX_SIZE = 50;
    private static final int FIRST_PAGE = 0;

    public CommunityChatHistoryCommand {
        if (size < 1 || size > MAX_SIZE) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
    }

    public static CommunityChatHistoryCommand of(Long userId, Long postId, Long cursor, int size) {
        return new CommunityChatHistoryCommand(userId, postId, cursor, size);
    }

    /**
     * 다음 페이지가 남았는지 알아내려고 요청 크기보다 한 건 더 조회한다.
     * count 쿼리로 전체를 세는 대신 초과분이 오는지만 보면 되고, 그 한 건은 응답에서 걷어낸다.
     */
    public Pageable toPageable() {
        return PageRequest.of(FIRST_PAGE, size + 1);
    }
}
