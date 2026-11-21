package com.team.cops_and_robbers.game.participant.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({"CHASER, CHASER", "POLICE, CHASER", "RUNNER, RUNNER", "ROBBER, RUNNER"})
    void 새_이름과_옛_이름을_모두_받는다(String value, Team expected) throws Exception {
        assertThat(objectMapper.readValue("\"" + value + "\"", Team.class)).isEqualTo(expected);
    }

    @Test
    void 응답은_옛_이름으로_내보낸다() throws Exception {
        assertThat(objectMapper.writeValueAsString(Team.CHASER)).isEqualTo("\"POLICE\"");
        assertThat(objectMapper.writeValueAsString(Team.RUNNER)).isEqualTo("\"ROBBER\"");
    }

    @Test
    void 모르는_이름이면_실패한다() {
        assertThatThrownBy(() -> Team.from("ZOMBIE")).isInstanceOf(IllegalArgumentException.class);
    }
}
