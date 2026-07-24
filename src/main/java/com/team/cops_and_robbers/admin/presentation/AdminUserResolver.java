package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.admin.application.AdminUserService;
import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.AdminUserListCommand;
import com.team.cops_and_robbers.admin.exception.AdminException;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.admin.application.dto.result.AdminUserPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.AdminUserResult;
import com.team.cops_and_robbers.admin.application.dto.result.GameParticipationResult;
import com.team.cops_and_robbers.admin.application.dto.result.UserDeviceResult;
import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.user.domain.SocialType;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminUserResolver {

    private final AdminUserService adminUserService;

    @QueryMapping
    public AdminUserPageResult adminUsers(
            @Argument int page,
            @Argument int size,
            @Argument String nickname,
            @Argument SocialType socialType,
            @Argument String fromDate,
            @Argument String toDate,
            @Argument SortDirection sortDirection
    ) {
        LocalDateTime parsedFromDate = parseDate(fromDate);
        LocalDateTime parsedToDate = parseDate(toDate);
        AdminUserListCommand command = new AdminUserListCommand(
                page, size, nickname, socialType, parsedFromDate, parsedToDate,
                sortDirection != null ? sortDirection : SortDirection.DESC);
        return adminUserService.getUserList(command);
    }

    @QueryMapping
    public AdminUserResult adminUser(@Argument Long id) {
        return adminUserService.getUser(id);
    }

    @BatchMapping(typeName = "AdminUser", field = "device")
    public Map<AdminUserResult, UserDeviceResult> device(List<AdminUserResult> users) {
        return adminUserService.getDevicesByUsers(users);
    }

    @BatchMapping(typeName = "AdminUser", field = "participations")
    public Map<AdminUserResult, List<GameParticipationResult>> participations(
            List<AdminUserResult> users) {
        return adminUserService.getGameParticipationsByUsers(users);
    }

    private LocalDateTime parseDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            return TimestampUtil.parseToKstLocalDateTime(dateString);
        } catch (DateTimeParseException e) {
            throw new ApplicationException(AdminException.INVALID_DATE_FORMAT);
        }
    }
}
