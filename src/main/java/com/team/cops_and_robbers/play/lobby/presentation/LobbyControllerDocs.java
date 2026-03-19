package com.team.cops_and_robbers.play.lobby.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.ReadyUpdateRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.TeamChangeRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.response.LobbyInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Lobby", description = "게임 로비 상태 변경 API")
public interface LobbyControllerDocs {

    @Operation(
            summary = "로비 팀 변경",
            description = "대기 상태(WAITING)에서만 팀을 변경할 수 있습니다. 팀 변경 시 해당 유저의 준비 상태는 해제됩니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiErrorCode(value = GameParticipantException.class, codes = {"GAME_ALREADY_STARTED", "LOBBY_ACTION_NOT_ALLOWED", "PARTICIPANT_NOT_FOUND"})
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_FOUND"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "팀 변경 성공")
    })
    ResponseEntity<Void> changeTeam(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId,
            @RequestBody @Valid TeamChangeRequest request
    );

    @Operation(
            summary = "로비 준비 상태 변경",
            description = "대기 상태(WAITING)에서만 준비 상태를 변경할 수 있습니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiErrorCode(value = GameParticipantException.class, codes = {"HOST_CANNOT_UNREADY", "GAME_ALREADY_STARTED", "LOBBY_ACTION_NOT_ALLOWED", "PARTICIPANT_NOT_FOUND"})
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_FOUND"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "준비 상태 변경 성공")
    })
    ResponseEntity<Void> updateReady(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId,
            @RequestBody @Valid ReadyUpdateRequest request
    );

    @Operation(
            summary = "게임 시작",
            description = """
                    대기 중인 게임을 시작합니다.

                    - 방장(Host)만 시작 가능
                    - 경찰/도둑 팀 각각 1명 이상 필요
                    - 모든 참가자가 준비(Ready) 상태여야 함
                    """,
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiErrorCode(value = GameParticipantException.class, codes = {"NOT_HOST", "INVALID_TEAM_COMPOSITION", "NOT_ALL_READY", "GAME_ALREADY_STARTED", "LOBBY_ACTION_NOT_ALLOWED", "PARTICIPANT_NOT_FOUND"})
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "게임 시작 성공")
    })
    ResponseEntity<Void> startGame(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );

    @Operation(
            summary = "로비 조회",
            description = "대기 상태(WAITING)인 게임의 로비 정보를 조회합니다. 요청자의 참가자 ID, 방장 ID, 전체 참가자 목록을 반환합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiErrorCode(value = GameParticipantException.class, codes = {"GAME_ALREADY_STARTED", "PARTICIPANT_NOT_FOUND"})
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로비 조회 성공")
    })
    ResponseEntity<LobbyInfoResponse> getLobbyInfo(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );

    @Operation(
            summary = "멤버 강제 퇴장",
            description = "방장이 대기실의 특정 참가자를 강제 퇴장합니다. 방장 본인은 강제 퇴장할 수 없습니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiErrorCode(value = GameParticipantException.class, codes = {"NOT_HOST", "CANNOT_KICK_YOURSELF", "GAME_ALREADY_STARTED", "LOBBY_ACTION_NOT_ALLOWED", "PARTICIPANT_NOT_FOUND"})
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "강제 퇴장 성공")
    })
    ResponseEntity<Void> kickMember(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId,
            @Parameter(description = "강제 퇴장할 참가자 ID", required = true, example = "2") @PathVariable Long participantId
    );
}
