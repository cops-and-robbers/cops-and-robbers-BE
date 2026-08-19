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

    private static final String POST_ID = "postId";
    private static final List<String> COMMUNITY_PATH_PATTERNS = List.of(
            "/publish/community/{"+ POST_ID +"}/**",
            "/subscribe/community/{"+ POST_ID +"}/**"
    );

    private static final AntPathMatcher matcher = new AntPathMatcher();


    public static Optional<Long> getGameId(String destination) {
        return extractId(GAME_PATH_PATTERNS, GAME_ID, destination);
    }

    public static Optional<Long> getPostId(String destination) {
        return extractId(COMMUNITY_PATH_PATTERNS, POST_ID, destination);
    }

    /**
     * id 파싱 성공 여부와 무관하게 커뮤니티 경로인지만 판별한다.
     * 라우팅 단계에서 쓰이며, id가 깨진 커뮤니티 경로가 게임 인터셉터로 새는 것을 막는다.
     */
    public static boolean isCommunityPath(String destination) {
        if (destination == null) return false;

        return COMMUNITY_PATH_PATTERNS.stream()
                .anyMatch(pattern -> matcher.match(pattern, destination));
    }

    private static Optional<Long> extractId(List<String> patterns, String variableName, String destination) {
        if (destination == null) return Optional.empty();

        return patterns.stream()
                .filter(pattern -> matcher.match(pattern, destination))
                .findFirst()
                .map(pattern -> matcher.extractUriTemplateVariables(pattern, destination))
                .map(variables -> variables.get(variableName))
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
