package com.team.cops_and_robbers.admin.application.dto.command.user;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.user.domain.SocialType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

public record AdminUserListCommand(
        int page,
        int size,
        String nickname,
        SocialType socialType,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        SortDirection sortDirection
) {

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(sortDirection.toSpringDirection(), "createdAt"));
    }
}
