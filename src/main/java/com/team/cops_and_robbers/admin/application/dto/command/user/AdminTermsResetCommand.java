package com.team.cops_and_robbers.admin.application.dto.command.user;

import com.team.cops_and_robbers.admin.exception.AdminException;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.user.domain.TermsType;
import org.springframework.util.CollectionUtils;

import java.util.List;

public record AdminTermsResetCommand(
        List<TermsType> types
) {

    public AdminTermsResetCommand {
        if (CollectionUtils.isEmpty(types)) {
            throw new ApplicationException(AdminException.EMPTY_TERMS_RESET_TARGET);
        }
    }

    public static AdminTermsResetCommand of(List<TermsType> types) {
        return new AdminTermsResetCommand(types);
    }
}
