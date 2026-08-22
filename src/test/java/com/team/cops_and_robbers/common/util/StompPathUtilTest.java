package com.team.cops_and_robbers.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StompPathUtilTest {

    @Nested
    @DisplayName("gameId 추출")
    class GetGameId {

        @Test
        void 게임_경로에서_gameId를_추출한다() {
            assertThat(StompPathUtil.getGameId("/publish/game/42/chat")).contains(42L);
            assertThat(StompPathUtil.getGameId("/subscribe/game/7/chat/all")).contains(7L);
            assertThat(StompPathUtil.getGameId("/subscribe/game/1/location/police")).contains(1L);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                "/publish/game/abc/chat",
                "/publish/community/1/chat",
                "/some/other/path"
        })
        void 추출할_수_없으면_empty를_반환한다(String destination) {
            assertThat(StompPathUtil.getGameId(destination)).isEmpty();
        }
    }

    @Nested
    @DisplayName("postId 추출")
    class GetPostId {

        @Test
        void 커뮤니티_경로에서_postId를_추출한다() {
            assertThat(StompPathUtil.getPostId("/publish/community/42/chat")).contains(42L);
            assertThat(StompPathUtil.getPostId("/subscribe/community/7/chat")).contains(7L);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                "/publish/community/abc/chat",
                "/publish/game/1/chat",
                "/some/other/path"
        })
        void 추출할_수_없으면_empty를_반환한다(String destination) {
            assertThat(StompPathUtil.getPostId(destination)).isEmpty();
        }
    }

    @Nested
    @DisplayName("커뮤니티 경로 판별")
    class IsCommunityPath {

        @ParameterizedTest
        @ValueSource(strings = {
                "/publish/community/1/chat",
                "/subscribe/community/42/chat"
        })
        void 커뮤니티_경로면_true를_반환한다(String destination) {
            assertThat(StompPathUtil.isCommunityPath(destination)).isTrue();
        }

        @Test
        void id가_깨진_커뮤니티_경로도_커뮤니티로_판별한다() {
            assertThat(StompPathUtil.isCommunityPath("/publish/community/abc/chat")).isTrue();
            assertThat(StompPathUtil.getPostId("/publish/community/abc/chat")).isEmpty();
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                "/publish/game/1/chat",
                "/subscribe/game/1/chat/all",
                "/publish/community",
                "/subscribe/community",
                "/some/other/path"
        })
        void 커뮤니티_경로가_아니면_false를_반환한다(String destination) {
            assertThat(StompPathUtil.isCommunityPath(destination)).isFalse();
        }
    }

    @Nested
    @DisplayName("게임 경로와 커뮤니티 경로의 상호 배타성")
    class Exclusivity {

        @ParameterizedTest
        @ValueSource(strings = {
                "/publish/game/1/chat",
                "/subscribe/game/1/chat/all",
                "/subscribe/game/1/system",
                "/subscribe/game/1/ping/police"
        })
        void 게임_경로는_커뮤니티로_분기되지_않는다(String destination) {
            assertThat(StompPathUtil.isCommunityPath(destination)).isFalse();
            assertThat(StompPathUtil.getGameId(destination)).isPresent();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/publish/community/1/chat",
                "/subscribe/community/1/chat"
        })
        void 커뮤니티_경로는_게임_인터셉터가_처리할_수_없다(String destination) {
            assertThat(StompPathUtil.getGameId(destination)).isEmpty();
            assertThat(StompPathUtil.isCommunityPath(destination)).isTrue();
        }
    }
}
