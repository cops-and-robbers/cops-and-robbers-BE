package com.team.cops_and_robbers.user.application.dto.command;

public record AgreementCommand(
        Long userId,
        Boolean termsOfService,
        Boolean privacyPolicy,
        Boolean locationTerms,
        Boolean marketing
) {
    public boolean hasRequiredTermsAgreed() {
        return termsOfService && privacyPolicy && locationTerms;
    }

    public static AgreementCommand of(
            Long userId, Boolean termsOfService,
            Boolean privacyPolicy, Boolean locationTerms,
            Boolean marketing
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
