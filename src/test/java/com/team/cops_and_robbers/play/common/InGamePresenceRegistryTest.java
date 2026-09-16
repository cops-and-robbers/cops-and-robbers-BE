package com.team.cops_and_robbers.play.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InGamePresenceRegistryTest {

    private static final Long GAME_ID = 1L;
    private static final Long PARTICIPANT_ID = 10L;

    private InGamePresenceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InGamePresenceRegistry();
    }

    @Test
    void 구독한_세션이_있으면_온라인이다() {
        registry.register(GAME_ID, PARTICIPANT_ID, "s1");

        assertThat(registry.isOnline(GAME_ID, PARTICIPANT_ID)).isTrue();
    }

    @Test
    void 등록된_적_없는_참가자는_오프라인이다() {
        assertThat(registry.isOnline(GAME_ID, PARTICIPANT_ID)).isFalse();
        assertThat(registry.isOnline(99L, PARTICIPANT_ID)).isFalse();
    }

    @Test
    void 세션이_끊기면_오프라인이_된다() {
        registry.register(GAME_ID, PARTICIPANT_ID, "s1");

        registry.unregister("s1");

        assertThat(registry.isOnline(GAME_ID, PARTICIPANT_ID)).isFalse();
    }

    @Test
    void 재연결로_세션이_겹치면_이전_세션이_끊겨도_온라인을_유지한다() {
        registry.register(GAME_ID, PARTICIPANT_ID, "old");
        registry.register(GAME_ID, PARTICIPANT_ID, "new");

        registry.unregister("old");

        assertThat(registry.isOnline(GAME_ID, PARTICIPANT_ID)).isTrue();
    }

    @Test
    void 같은_세션이_채널_여러_개를_구독해도_한_번만_센다() {
        registry.register(GAME_ID, PARTICIPANT_ID, "s1");
        registry.register(GAME_ID, PARTICIPANT_ID, "s1");

        registry.unregister("s1");

        assertThat(registry.isOnline(GAME_ID, PARTICIPANT_ID)).isFalse();
    }

    @Test
    void 모르는_세션을_끊어도_아무_일도_없다() {
        registry.register(GAME_ID, PARTICIPANT_ID, "s1");

        registry.unregister("unknown");

        assertThat(registry.isOnline(GAME_ID, PARTICIPANT_ID)).isTrue();
    }
}
