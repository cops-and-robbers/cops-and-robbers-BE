package com.team.cops_and_robbers.moderation.presentation.validation;

import com.team.cops_and_robbers.moderation.application.ProfanityFilter;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NoBannedWordValidator implements ConstraintValidator<NoBannedWord, String> {

    private final ProfanityFilter profanityFilter;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return !profanityFilter.containsBannedWord(value);
    }
}
