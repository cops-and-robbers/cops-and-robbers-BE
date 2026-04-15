package com.team.cops_and_robbers.user.application.dto.command;

public record AgreementCommand(
        Long userId,
        boolean termsOfService,
        boolean privacyPolicy,
        boolean locationTerms,
        boolean marketing
) {
    public boolean hasRequiredTermsAgreed() {
        return termsOfService && privacyPolicy && locationTerms;
    }

    public static AgreementCommand of(
            Long userId, boolean termsOfService,
            boolean privacyPolicy, boolean locationTerms,
            boolean marketing
    ) {
        return new AgreementCommand(
                userId,
                termsOfService,
                privacyPolicy,
                locationTerms,
                marketing
        );
    }
}
