package com.team.cops_and_robbers.admin.application.dto.result;

import com.team.cops_and_robbers.user.domain.User;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserPageResult(
        List<AdminUserResult> content,
        Long totalElements,
        Integer totalPages,
        Integer page,
        Integer size
) {
    public static AdminUserPageResult from(Page<User> userPage) {
        List<AdminUserResult> content = userPage.getContent().stream()
                .map(AdminUserResult::from)
                .toList();
        return new AdminUserPageResult(
                content,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.getNumber(),
                userPage.getSize()
        );
    }
}
