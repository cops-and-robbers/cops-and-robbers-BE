package com.team.cops_and_robbers.admin.application.dto.result.dashboard;

import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.repository.EndReasonCountProjection;

public record EndReasonDistributionResult(
        GameEndReason endReason,
        Long count
) {

    public static EndReasonDistributionResult from(EndReasonCountProjection projection) {
        return new EndReasonDistributionResult(projection.getEndReason(), projection.getCount());
    }
}
