package com.team.cops_and_robbers.user.repository;

import static com.team.cops_and_robbers.user.domain.QUser.user;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import com.team.cops_and_robbers.user.domain.Role;
import com.team.cops_and_robbers.user.domain.TermsType;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countTermsResetTargets(List<TermsType> types) {
        Long count = queryFactory
                .select(user.count())
                .from(user)
                .where(agreedToAny(types), isNotAdmin())
                .fetchOne();
        return count == null ? 0L : count;
    }

    @Override
    public long resetTermsAgreement(List<TermsType> types) {
        JPAUpdateClause update = queryFactory.update(user);
        types.forEach(type -> clearAgreement(update, type));
        return update.where(agreedToAny(types), isNotAdmin()).execute();
    }

    private void clearAgreement(JPAUpdateClause update, TermsType type) {
        switch (type) {
            case TERMS_OF_SERVICE -> update.set(user.termsOfServiceAgreed, false);
            case PRIVACY_POLICY -> update.set(user.privacyPolicyAgreed, false);
            case LOCATION_TERMS -> update.set(user.locationTermsAgreed, false);
            case MARKETING -> update.set(user.allowMarketingPush, false).setNull(user.marketingAgreedAt);
        }
    }

    /** 어드민은 운영자 계정이라 재동의 대상에서 뺀다. */
    private BooleanExpression isNotAdmin() {
        return user.role.ne(Role.ADMIN);
    }

    /**
     * 조건 없이 update가 나가면 전체 사용자를 건드리므로, 빈 목록은 아무도 고르지 않게 막는다.
     */
    private BooleanExpression agreedToAny(List<TermsType> types) {
        return types.stream()
                .map(this::agreed)
                .reduce(BooleanExpression::or)
                .orElse(Expressions.FALSE);
    }

    private BooleanExpression agreed(TermsType type) {
        return switch (type) {
            case TERMS_OF_SERVICE -> user.termsOfServiceAgreed.isTrue();
            case PRIVACY_POLICY -> user.privacyPolicyAgreed.isTrue();
            case LOCATION_TERMS -> user.locationTermsAgreed.isTrue();
            case MARKETING -> user.allowMarketingPush.isTrue();
        };
    }
}
