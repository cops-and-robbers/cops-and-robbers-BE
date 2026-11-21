package com.team.cops_and_robbers.play.system.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.cops_and_robbers.play.system.application.dto.result.ArrestResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ArrestResponse(
        @Schema(description = "체포된 도둑 닉네임", example = "잡힌도둑")
        String runnerNickname,
        @Schema(description = "남은 도둑 수", example = "2")
        int remainingRunners
) {
    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "robberNickname", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. runnerNickname 을 사용하세요", deprecated = true)
    public String robberNickname() {
        return runnerNickname;
    }

    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "remainingThieves", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. remainingRunners 을 사용하세요", deprecated = true)
    public int remainingThieves() {
        return remainingRunners;
    }

    public static ArrestResponse from(ArrestResult result) {
        return new ArrestResponse(
                result.runnerNickname(),
                result.remainingRunners()
        );
    }
}
