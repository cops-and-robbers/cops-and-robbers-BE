package com.team.cops_and_robbers.history.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.history.exception.GameResultException;
import com.team.cops_and_robbers.history.presentation.dto.response.GameResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Game Result", description = "게임 결과 조회 API")
public interface GameResultControllerDocs {

    @Operation(
            summary = "게임 결과 조회",
            description = "게임 종료 후 결과(승리 팀, 진행 시간, 체포 횟수, 남은 도둑 수)를 조회합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiErrorCode(value = GameResultException.class, codes = {"GAME_RESULT_NOT_FOUND"})
    @ApiErrorCode(value = GameParticipantException.class, codes = {"PARTICIPANT_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게임 결과 조회 성공")
    })
    ResponseEntity<GameResultResponse> getGameResult(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 결과 ID", required = true, example = "1") @PathVariable Long gameResultId
    );
}
