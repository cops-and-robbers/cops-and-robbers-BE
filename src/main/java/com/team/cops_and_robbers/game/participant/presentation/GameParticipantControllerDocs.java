package com.team.cops_and_robbers.game.participant.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.presentation.dto.request.GameJoinRequest;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameJoinResponse;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameLeaveResponse;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameParticipantListResponse;
import com.team.cops_and_robbers.user.exception.UserException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Game Participant", description = "게임 참여자 관리 API")
public interface GameParticipantControllerDocs {

    @Operation(summary = "게임 방 참여",
            description = """
                    초대 코드를 사용하여 게임 방에 참여합니다.

                    - **일반 게임**: 게임이 WAITING 상태일 때만 입장 가능하며, 최대 인원 초과 시 참여 불가
                    - **이벤트 게임** (`isEventGame: true`): 게임이 IN_PROGRESS 상태에서도 자유롭게 입장 가능하며 인원 제한 없음. 항상 경찰 팀으로 배정됨.

                    응답의 `isEventGame` 필드가 `true`이면 클라이언트는 로비 화면 없이 인게임 화면으로 직접 진입해야 합니다.
                    """
    )
    @ApiErrorCode(value = GameParticipantException.class, codes = {"ALREADY_PARTICIPATING", "GAME_ALREADY_STARTED", "GAME_FULL", "INVALID_INVITE_CODE"})
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_IN_PROGRESS"})
    @ApiErrorCode(value = UserException.class, codes = {"REQUIRED_TERMS_NOT_AGREED"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게임 참여 성공")
    })
    ResponseEntity<GameJoinResponse> joinGame(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid GameJoinRequest request
    );

    @Operation(summary = "게임 방 퇴장",
            description = """
                    게임 상태(로비 / 인게임)에 따라 퇴장 처리가 다르게 동작합니다.

                    ---

                    **[로비 상태 (WAITING)]**

                    - 방장이 퇴장하면 가장 먼저 참여한 참여자에게 방장이 이전됩니다.
                      → `/subscribe/game/{gameId}/lobby` 채널로 `HOST_CHANGED` 이벤트 수신
                    - 마지막 참여자가 퇴장하면 게임 방이 삭제됩니다. (이벤트 없음)
                    - 그 외 일반 퇴장: `/subscribe/game/{gameId}/lobby` 채널로 `EXIT` 이벤트 수신

                    ---

                    **[인게임 상태 (IN_PROGRESS)]**

                    퇴장자의 팀과 상태에 따라 아래 네 가지 케이스로 분기됩니다.

                    ■ 경찰 퇴장 & 경찰이 아직 남아 있는 경우
                    → `/subscribe/game/{gameId}/system` 채널로 `PLAYER_LEFT` 이벤트 수신

                    ■ 경찰 퇴장 & 마지막 경찰인 경우 (도둑팀 몰수(forfeit) 승리)
                    → `/subscribe/game/{gameId}/system` 채널로 `GAME_OVER` 이벤트 수신
                    ```json
                    { "winnerTeam": "ROBBER", "reason": "POLICE_FORFEITED" }
                    ```

                    ■ 생존(ALIVE) 도둑 퇴장 & 생존 도둑이 아직 남아 있는 경우
                    → `/subscribe/game/{gameId}/system` 채널로 `PLAYER_LEFT` 이벤트 수신

                    ■ 생존(ALIVE) 도둑 퇴장 & 마지막 생존 도둑인 경우 (경찰팀 forfeit 승리)
                    → `/subscribe/game/{gameId}/system` 채널로 `GAME_OVER` 이벤트 수신
                    ```json
                    { "winnerTeam": "POLICE", "reason": "ROBBER_FORFEITED" }
                    ```

                    ■ JAILED 도둑 퇴장 (게임 종료 조건 무관)
                    → `/subscribe/game/{gameId}/system` 채널로 `PLAYER_LEFT` 이벤트 수신
                    (JAILED 상태의 도둑은 생존 도둑 카운트에 포함되지 않으므로 forfeit 조건을 체크하지 않습니다.)

                    ---

                    **[PLAYER_LEFT 이벤트 페이로드]**
                    ```json
                    {
                      "eventId": "550e8400-e29b-41d4-a716-446655440000",
                      "gameId": 1,
                      "type": "PLAYER_LEFT",
                      "timestamp": "2026-06-15T10:00:00+09:00",
                      "data": {
                        "participantId": 42,
                        "nickname": "닉네임",
                        "team": "POLICE 또는 ROBBER"
                      }
                    }
                    ```

                    **[GAME_OVER 이벤트 페이로드]**
                    ```json
                    {
                      "eventId": "550e8400-e29b-41d4-a716-446655440001",
                      "gameId": 1,
                      "type": "GAME_OVER",
                      "timestamp": "2026-06-15T10:00:05+09:00",
                      "data": {
                        "gameResultId": 1,
                        "winnerTeam": "POLICE 또는 ROBBER",
                        "reason": "POLICE_FORFEITED 또는 ROBBER_FORFEITED"
                      }
                    }
                    ```

                    ---

                    **[방장 위임]**

                    방장이 퇴장하는 경우, 게임 상태(로비/인게임)와 무관하게 다음 참여자에게 방장이 이전됩니다.
                    → `/subscribe/game/{gameId}/lobby` 채널로 `HOST_CHANGED` 이벤트 추가 수신
                    """
    )
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_FOUND"})
    @ApiErrorCode(value = GameParticipantException.class, codes = {"PARTICIPANT_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게임 퇴장 성공")
    })
    ResponseEntity<GameLeaveResponse> leaveGame(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );

    @Operation(
            summary = "게임 참가자 인게임 상태 목록 조회",
            description = """
                    경찰/도둑 팀별 참가자 목록과 상태를 조회합니다.

                    - 게임이 진행 중(IN_PROGRESS) 상태에서만 조회 가능
                    - 해당 게임의 참가자만 조회 가능
                    - 경찰 상태: POLICE_WAITING(대기 중), ALIVE(활성)
                    - 도둑 상태: ALIVE(생존), JAILED(잡힘)
                    """
    )
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_FOUND", "GAME_NOT_IN_PROGRESS"})
    @ApiErrorCode(value = GameParticipantException.class, codes = {"PARTICIPANT_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참가자 목록 조회 성공")
    })
    ResponseEntity<GameParticipantListResponse> getParticipantList(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );
}
