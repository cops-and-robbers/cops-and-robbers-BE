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
            description = "초대 코드를 사용하여 게임 방에 참여합니다. 이미 다른 활성 게임에 참여 중이거나, 게임이 이미 시작되었거나, 최대 참여 인원에 도달한 경우 참여할 수 없습니다."
    )
    @ApiErrorCode(value = GameParticipantException.class, codes = {"ALREADY_PARTICIPATING", "GAME_ALREADY_STARTED", "GAME_FULL", "INVALID_INVITE_CODE"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게임 참여 성공")
    })
    ResponseEntity<GameJoinResponse> joinGame(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid GameJoinRequest request
    );

    @Operation(summary = "게임 방 퇴장",
            description = "현재 참여 중인 게임 방에서 퇴장합니다. 방장이 퇴장하는 경우 가장 먼저 참여한 참여자에게 방장 권한이 이전됩니다. 마지막 참여자가 퇴장하면 게임 방이 자동으로 삭제됩니다."
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
