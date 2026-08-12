package com.team.cops_and_robbers.admin.application.dto.command.game;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record AdminGameHistoryListCommand(
        int page,
        int size,
        GameEndReason endReason,
        SortDirection sortDirection
) {

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(sortDirection.toSpringDirection(), "createdAt"));
    }
}
