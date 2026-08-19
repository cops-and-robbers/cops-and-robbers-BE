package com.team.cops_and_robbers.auth.application;

import com.team.cops_and_robbers.auth.domain.NicknameLanguage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomNicknameGenerator {

    private static final int NICKNAME_SUFFIX_BOUND = 10_000;
    private static final String NICKNAME_SUFFIX_FORMAT = "%04d";

    public String generate(NicknameLanguage language) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<String> adjectives = language.adjectives();
        List<String> animals = language.animals();

        String adjective = adjectives.get(random.nextInt(adjectives.size()));
        String animal = animals.get(random.nextInt(animals.size()));
        String suffix = NICKNAME_SUFFIX_FORMAT.formatted(random.nextInt(NICKNAME_SUFFIX_BOUND));

        return adjective + animal + suffix;
    }
}