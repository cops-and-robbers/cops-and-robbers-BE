package com.team.cops_and_robbers.play.lobby.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.ReadyUpdateRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.TeamChangeRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.response.LobbyInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "팀 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "요청 바디 검증 실패",
                                            value = """
                                                    {
                                                        "title": "유효하지 않은 입력값",
                                                        "status": 400,
                                                        "detail": "targetTeam: 팀은 필수입니다.",
                                                        "instance": "/api/games/1/lobby/team"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "게임이 이미 시작됨",
                                            value = """
                                                    {
                                                        "title": "이미 시작된 게임",
                                                        "status": 400,
                                                        "detail": "이미 시작된 게임에는 참여할 수 없습니다.",
                                                        "instance": "/api/games/1/lobby/team"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "로비 조작 불가",
                                            value = """
                                                    {
                                                        "title": "로비 조작 불가",
                                                        "status": 400,
                                                        "detail": "게임이 시작된 이후에는 로비 상태를 변경할 수 없습니다.",
                                                        "instance": "/api/games/1/lobby/team"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "인증 토큰 없음 또는 만료",
                                    value = """
                                            {
                                                "title": "인증 필요",
                                                "status": 401,
                                                "detail": "로그인이 필요한 서비스입니다.",
                                                "instance": "/api/games/1/lobby/team"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "게임 또는 참여 정보 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "존재하지 않는 게임",
                                            value = """
                                                    {
                                                        "title": "게임을 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "해당 게임을 찾을 수 없습니다.",
                                                        "instance": "/api/games/999/lobby/team"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "참가자를 찾을 수 없음",
                                            value = """
                                                    {
                                                        "title": "참가자를 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "해당 게임에 참가하지 않은 사용자입니다.",
                                                        "instance": "/api/games/1/lobby/team"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<Void> changeTeam(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "팀 변경 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TeamChangeRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "경찰로 변경",
                                            value = """
                                                    {
                                                        "targetTeam": "POLICE"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "도둑으로 변경",
                                            value = """
                                                    {
                                                        "targetTeam": "ROBBER"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @RequestBody @Valid TeamChangeRequest request
    );

    @Operation(
            summary = "로비 준비 상태 변경",
            description = "대기 상태(WAITING)에서만 준비 상태를 변경할 수 있습니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "준비 상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "요청 바디 검증 실패",
                                            value = """
                                                    {
                                                        "title": "유효하지 않은 입력값",
                                                        "status": 400,
                                                        "detail": "isReady: 준비 여부는 필수입니다.",
                                                        "instance": "/api/games/1/lobby/ready"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "방장 레디 해제 불가",
                                            value = """
                                                    {
                                                        "title": "방장 레디 해제 불가",
                                                        "status": 400,
                                                        "detail": "방장은 항상 준비 상태여야 합니다.",
                                                        "instance": "/api/games/1/lobby/ready"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "게임이 이미 시작됨",
                                            value = """
                                                    {
                                                        "title": "이미 시작된 게임",
                                                        "status": 400,
                                                        "detail": "이미 시작된 게임에는 참여할 수 없습니다.",
                                                        "instance": "/api/games/1/lobby/ready"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "로비 조작 불가",
                                            value = """
                                                    {
                                                        "title": "로비 조작 불가",
                                                        "status": 400,
                                                        "detail": "게임이 시작된 이후에는 로비 상태를 변경할 수 없습니다.",
                                                        "instance": "/api/games/1/lobby/ready"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "인증 토큰 없음 또는 만료",
                                    value = """
                                            {
                                                "title": "인증 필요",
                                                "status": 401,
                                                "detail": "로그인이 필요한 서비스입니다.",
                                                "instance": "/api/games/1/lobby/ready"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "게임 또는 참여 정보 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "존재하지 않는 게임",
                                            value = """
                                                    {
                                                        "title": "게임을 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "해당 게임을 찾을 수 없습니다.",
                                                        "instance": "/api/games/999/lobby/ready"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "참가자를 찾을 수 없음",
                                            value = """
                                                    {
                                                        "title": "참가자를 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "해당 게임에 참가하지 않은 사용자입니다.",
                                                        "instance": "/api/games/1/lobby/ready"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<Void> updateReady(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "준비 상태 변경 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ReadyUpdateRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "준비 상태 ON",
                                            value = """
                                                    {
                                                        "isReady": true
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "준비 상태 OFF",
                                            value = """
                                                    {
                                                        "isReady": false
                                                    }
                                                    """
                                    )
                            }
                    )
            )
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "게임 시작 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "방장이 아님",
                                            value = """
                                                    {
                                                        "title": "권한 없음",
                                                        "status": 400,
                                                        "detail": "게임을 시작할 수 있는 권한이 없습니다. (방장만 가능)",
                                                        "instance": "/api/games/1/lobby/start"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "팀 구성 오류",
                                            value = """
                                                    {
                                                        "title": "팀 구성 오류",
                                                        "status": 400,
                                                        "detail": "경찰과 도둑 팀에 각각 최소 1명 이상의 참가자가 필요합니다.",
                                                        "instance": "/api/games/1/lobby/start"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "모든 참가자가 준비하지 않음",
                                            value = """
                                                    {
                                                        "title": "준비되지 않은 참가자 존재",
                                                        "status": 400,
                                                        "detail": "모든 참가자가 준비 상태여야 게임을 시작할 수 있습니다.",
                                                        "instance": "/api/games/1/lobby/start"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "게임이 이미 시작됨",
                                            value = """
                                                    {
                                                        "title": "이미 시작된 게임",
                                                        "status": 400,
                                                        "detail": "이미 시작된 게임에는 참여할 수 없습니다.",
                                                        "instance": "/api/games/1/lobby/start"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "로비 조작 불가",
                                            value = """
                                                    {
                                                        "title": "로비 조작 불가",
                                                        "status": 400,
                                                        "detail": "게임이 시작된 이후에는 로비 상태를 변경할 수 없습니다.",
                                                        "instance": "/api/games/1/lobby/start"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "인증 토큰 없음 또는 만료",
                                    value = """
                                            {
                                                "title": "인증 필요",
                                                "status": 401,
                                                "detail": "로그인이 필요한 서비스입니다.",
                                                "instance": "/api/games/1/lobby/start"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "게임 또는 참여 정보 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "존재하지 않는 게임",
                                            value = """
                                                    {
                                                        "title": "게임을 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "해당 게임을 찾을 수 없습니다.",
                                                        "instance": "/api/games/999/lobby/start"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "참가자를 찾을 수 없음",
                                            value = """
                                                    {
                                                        "title": "참가자를 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "해당 게임에 참가하지 않은 사용자입니다.",
                                                        "instance": "/api/games/1/lobby/start"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<Void> startGame(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );

    @Operation(
            summary = "로비 조회",
            description = "대기 상태(WAITING)인 게임의 로비 정보를 조회합니다. 요청자의 참가자 ID, 방장 ID, 방 설정, 전체 참가자 목록을 반환합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로비 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = LobbyInfoResponse.class),
                            examples = @ExampleObject(
                                    name = "로비 조회 성공",
                                    value = """
                                            {
                                                "myParticipantId": 2,
                                                "hostParticipantId": 1,
                                                "maxParticipants": 10,
                                                "locationRevealIntervalMinutes": 3,
                                                "participants": [
                                                    {
                                                        "participantId": 1,
                                                        "nickname": "방장닉네임",
                                                        "team": "POLICE",
                                                        "isReady": true
                                                    },
                                                    {
                                                        "participantId": 2,
                                                        "nickname": "내닉네임",
                                                        "team": "ROBBER",
                                                        "isReady": false
                                                    }
                                                ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "게임이 대기 상태가 아님",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "게임이 이미 시작됨",
                                    value = """
                                            {
                                                "title": "이미 시작된 게임",
                                                "status": 400,
                                                "detail": "이미 시작된 게임에는 참여할 수 없습니다.",
                                                "instance": "/api/games/1/lobby"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "인증 토큰 없음 또는 만료",
                                    value = """
                                            {
                                                "title": "인증 필요",
                                                "status": 401,
                                                "detail": "로그인이 필요한 서비스입니다.",
                                                "instance": "/api/games/1/lobby"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "게임 또는 참여 정보 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "존재하지 않는 게임",
                                            value = """
                                                    {
                                                        "title": "게임을 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "요청하신 게임 정보가 존재하지 않습니다.",
                                                        "instance": "/api/games/999/lobby"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "참가자를 찾을 수 없음",
                                            value = """
                                                    {
                                                        "title": "참가자를 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "해당 게임에 참가하지 않은 사용자입니다.",
                                                        "instance": "/api/games/1/lobby"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<LobbyInfoResponse> getLobbyInfo(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );
}
