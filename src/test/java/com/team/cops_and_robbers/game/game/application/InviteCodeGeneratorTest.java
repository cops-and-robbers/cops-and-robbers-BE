package com.team.cops_and_robbers.game.game.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class InviteCodeGeneratorTest {

    @RepeatedTest(100)
    @DisplayName("생성된 초대 코드는 항상 6자리여야 한다")
    void 초대_코드_길이_검증() {
        // when
        String code = InviteCodeGenerator.generate();

        // then
        assertThat(code).hasSize(6);
    }

    @RepeatedTest(100)
    @DisplayName("100번의 생성 기회 동안 혼동을 줄 수 있는 문자(1, I, 0, O)가 한 번도 포함되지 않아야 한다")
    void 초대_코드_유효_문자_검증() {
        // when
        String code = InviteCodeGenerator.generate();

        // then
        assertThat(code)
                .doesNotContain("1")
                .doesNotContain("I")
                .doesNotContain("0")
                .doesNotContain("O");
    }

    @Test
    @DisplayName("100개를 동시에 생성했을 때 중복이 발생하지 않아야 한다")
    void 초대_코드_중복_확률_검증() {
        // given
        int testCount = 100;

        // when
        Set<String> codes = IntStream.range(0, testCount)
                .mapToObj(i -> InviteCodeGenerator.generate())
                .collect(Collectors.toSet());

        // then
        assertThat(codes).hasSize(testCount);
    }
}
