package com.team.cops_and_robbers.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StompPathUtil {

    private static final String GAME_ID = "gameId";
    private static final List<String> GAME_PATH_PATTERNS = List.of(
            "/publish/game/{"+ GAME_ID +"}/**",
            "/subscribe/game/{"+ GAME_ID +"}/**"
    );

    private static final AntPathMatcher matcher = new AntPathMatcher();


    public static Optional<Long> getGameId(String destination) {
        if (destination == null) return Optional.empty();

        return GAME_PATH_PATTERNS.stream()
                .filter(pattern -> matcher.match(pattern, destination))
                .findFirst()
                .map(pattern -> matcher.extractUriTemplateVariables(pattern, destination))
                .map(variables -> variables.get(GAME_ID))
                .flatMap(StompPathUtil::parseId);
    }

    private static Optional<Long> parseId(String idString) {
        if (idString.isEmpty()) return Optional.empty();

        try {
            return Optional.of(Long.parseLong(idString));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
