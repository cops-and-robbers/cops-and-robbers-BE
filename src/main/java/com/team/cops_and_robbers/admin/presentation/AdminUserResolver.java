package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.admin.application.AdminUserService;
import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.user.AdminUserListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminUserPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminUserResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.GameParticipationResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.UserDeviceResult;
import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.user.domain.SocialType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Validated
@Controller
@RequiredArgsConstructor
public class AdminUserResolver {

    private final AdminUserService adminUserService;

    @QueryMapping
    public AdminUserPageResult adminUsers(
            @Argument @Min(0) int page,
            @Argument @Min(1) @Max(100) int size,
            @Argument String nickname,
            @Argument SocialType socialType,
            @Argument OffsetDateTime fromDate,
            @Argument OffsetDateTime toDate,
            @Argument SortDirection sortDirection
    ) {
        AdminUserListCommand command = new AdminUserListCommand(
                page, size, nickname, socialType,
                TimestampUtil.toKstLocalDateTime(fromDate),
                TimestampUtil.toKstLocalDateTime(toDate),
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
}
