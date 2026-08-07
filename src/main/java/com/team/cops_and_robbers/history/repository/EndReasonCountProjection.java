package com.team.cops_and_robbers.history.repository;

import com.team.cops_and_robbers.history.domain.GameEndReason;

public interface EndReasonCountProjection {
    GameEndReason getEndReason();
    Long getCount();
}
