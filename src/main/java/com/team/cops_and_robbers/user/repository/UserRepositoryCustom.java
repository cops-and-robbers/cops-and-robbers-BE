package com.team.cops_and_robbers.user.repository;

import com.team.cops_and_robbers.user.domain.TermsType;

import java.util.List;

public interface UserRepositoryCustom {

    long countTermsResetTargets(List<TermsType> types);

    long resetTermsAgreement(List<TermsType> types);
}
