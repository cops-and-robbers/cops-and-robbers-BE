package com.team.cops_and_robbers.history.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.history.exception.GameResultException;
import com.team.cops_and_robbers.history.presentation.dto.response.GameResultParticipantResponse;
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
            description = "게임 종료 후 결과(승리 팀, 진행 시간, 총 체포 횟수, 남은 도둑 수)를 조회합니다.",
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

    @Operation(
            summary = "내 개인 기록 조회",
            description = """
                    그 게임에서 본인의 개인 기록(팀, 종료 시점 상태, 체포 횟수, 잡힌 횟수, 퇴장 시각)을 조회합니다.

                    요청한 사용자 자신의 기록만 반환하므로 participantId 를 넘기지 않습니다.
                    명단에 없으면 그 게임 참가자가 아니므로 `GAME_RESULT_NOT_FOUND` 입니다.

                    `arrestCount` 는 도둑과 이 기능이 추가되기 전에 끝난 게임에서는 `0` 입니다.
                    `arrestedCount` 는 경찰과 이 컬럼이 추가되기 전에 끝난 게임에서는 `0` 입니다.
                    두 필드를 모두 주므로 앱은 `team` 으로 골라 씁니다.

                    게임을 나갔다가 같은 방에 다시 들어온 경우, 재입장 이후의 기록만 반환합니다.
                    나가기 전에 잡은 수는 이어지지 않습니다.
                    """,
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiErrorCode(value = GameResultException.class, codes = {"GAME_RESULT_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "개인 기록 조회 성공")
    })
    ResponseEntity<GameResultParticipantResponse> getMyGameRecord(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 결과 ID", required = true, example = "1") @PathVariable Long gameResultId
    );
}
