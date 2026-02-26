package com.team.cops_and_robbers.game.participant.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.game.participant.presentation.dto.request.GameJoinRequest;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameJoinResponse;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameLeaveResponse;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameParticipantListResponse;
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

@Tag(name = "Game Participant", description = "게임 참여자 관리 API")
public interface GameParticipantControllerDocs {

    @Operation(summary = "게임 방 참여",
            description = "초대 코드를 사용하여 게임 방에 참여합니다. 이미 다른 활성 게임에 참여 중이거나, 게임이 이미 시작되었거나, 최대 참여 인원에 도달한 경우 참여할 수 없습니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게임 참여 성공",
                    content = @Content(
                            schema = @Schema(implementation = GameJoinResponse.class),
                            examples = @ExampleObject(
                                    name = "게임 참여 성공 예시",
                                    value = """
                                            {
                                                "gameId": 1,
                                                "participantId": 2
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "초대 코드 누락",
                                            value = """
                                                    {
                                                        "title": "유효하지 않은 입력값",
                                                        "status": 400,
                                                        "detail": "inviteCode: 초대 코드는 필수입니다.",
                                                        "instance": "/api/games/join"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "게임이 이미 시작됨",
                                            value = """
                                                    {
                                                        "title": "게임 참여 불가",
                                                        "status": 400,
                                                        "detail": "이미 시작된 게임에는 참여할 수 없습니다.",
                                                        "instance": "/api/games/join"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "최대 참여 인원 초과",
                                            value = """
                                                    {
                                                        "title": "게임 참여 불가",
                                                        "status": 400,
                                                        "detail": "게임 방의 최대 참여 인원에 도달했습니다.",
                                                        "instance": "/api/games/join"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "잘못된 초대 코드",
                                            value = """
                                                    {
                                                        "title": "초대 코드 오류",
                                                        "status": 400,
                                                        "detail": "입력하신 초대 코드가 유효하지 않습니다.",
                                                        "instance": "/api/games/join"
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
                                                "instance": "/api/games/join"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "409", description = "이미 다른 활성 게임에 참여 중인 경우",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "중복 게임 참여",
                                    value = """
                                            {
                                                "title": "이미 참가 중인 게임",
                                                "status": 409,
                                                "detail": "이미 게임에 참가하고 있습니다.",
                                                "instance": "/api/games/join"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<GameJoinResponse> joinGame(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "게임 참여 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GameJoinRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "정상 요청 예시",
                                            value = """
                                                    {
                                                        "inviteCode": "ABC123"
                                                    }
                                                    """
                                    )
                            }
                    )
            ) @RequestBody @Valid GameJoinRequest request
    );

    @Operation(summary = "게임 방 퇴장",
            description = "현재 참여 중인 게임 방에서 퇴장합니다. 방장이 퇴장하는 경우 가장 먼저 참여한 참여자에게 방장 권한이 이전됩니다. 마지막 참여자가 퇴장하면 게임 방이 자동으로 삭제됩니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게임 퇴장 성공",
                    content = @Content(
                            schema = @Schema(implementation = GameLeaveResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "일반 참여자 퇴장",
                                            value = """
                                                    {
                                                        "leftUserId": 2,
                                                        "remainingCount": 5
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "마지막 참여자 퇴장 (방 삭제)",
                                            value = """
                                                    {
                                                        "leftUserId": 1,
                                                        "remainingCount": 0
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
                                                "instance": "/api/games/1/leave"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "게임 또는 참여 정보를 찾을 수 없음",
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
                                                        "instance": "/api/games/999/leave"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "참여하지 않은 게임",
                                            value = """
                                                    {
                                                        "title": "참여 정보를 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "해당 게임에 참여하고 있지 않습니다.",
                                                        "instance": "/api/games/1/leave"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<GameLeaveResponse> leaveGame(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );

    @Operation(
            summary = "게임 참가자 목록 조회",
            description = """
                    경찰/도둑 팀별 참가자 목록과 상태를 조회합니다.

                    - 게임이 진행 중(IN_PROGRESS) 상태에서만 조회 가능
                    - 해당 게임의 참가자만 조회 가능
                    - 경찰 상태: POLICE_WAITING(대기 중), ALIVE(활성)
                    - 도둑 상태: ALIVE(생존), JAILED(잡힘)
                    """,
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참가자 목록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = GameParticipantListResponse.class),
                            examples = @ExampleObject(
                                    name = "참가자 목록 조회 성공 예시",
                                    value = """
                                            {
                                                "police": [
                                                    { "participantId": 1, "nickname": "경찰1", "status": "POLICE_WAITING" },
                                                    { "participantId": 2, "nickname": "경찰2", "status": "ALIVE" }
                                                ],
                                                "robbers": [
                                                    { "participantId": 3, "nickname": "도둑1", "status": "ALIVE" },
                                                    { "participantId": 4, "nickname": "도둑2", "status": "JAILED" }
                                                ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "게임 미진행",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "게임 미진행",
                                    value = """
                                            {
                                                "title": "게임 진행 중 아님",
                                                "status": 400,
                                                "detail": "게임이 진행 중인 상태가 아닙니다.",
                                                "instance": "/api/games/1/participants"
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
                                                "instance": "/api/games/1/participants"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "게임 또는 참가자 정보 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "게임을 찾을 수 없음",
                                            value = """
                                                    {
                                                        "title": "게임을 찾을 수 없음",
                                                        "status": 404,
                                                        "detail": "요청하신 게임 정보가 존재하지 않습니다.",
                                                        "instance": "/api/games/999/participants"
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
                                                        "instance": "/api/games/1/participants"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<GameParticipantListResponse> getParticipantList(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );
}
