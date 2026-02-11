package com.team.cops_and_robbers.play.system.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.play.system.presentation.dto.request.ArrestRequest;
import com.team.cops_and_robbers.play.system.presentation.dto.response.ArrestResponse;
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

@Tag(name = "System", description = "게임 시스템 상호작용 API")
public interface SystemControllerDocs {

    @Operation(
            summary = "도둑 체포",
            description = """
                    경찰이 도둑을 체포합니다.

                    - 경찰만 요청 가능
                    - 도둑만 체포 대상이 될 수 있음
                    - 이미 체포된 도둑은 체포 불가
                    - 같은 게임 내 참가자끼리만 상호작용 가능
                    """,
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "체포 성공",
                    content = @Content(schema = @Schema(implementation = ArrestResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "게임 진행 중 아님",
                                            value = """
                                                    {
                                                        "title": "게임 진행 중 아님",
                                                        "status": 400,
                                                        "detail": "게임이 진행 중인 상태가 아닙니다.",
                                                        "instance": "/api/games/1/system/arrest"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "경찰이 아님",
                                            value = """
                                                    {
                                                        "title": "경찰만 체포 가능",
                                                        "status": 400,
                                                        "detail": "경찰 팀만 도둑을 체포할 수 있습니다.",
                                                        "instance": "/api/games/1/system/arrest"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "경찰 대기 시간",
                                            value = """
                                                    {
                                                        "title": "경찰 대기 시간",
                                                        "status": 400,
                                                        "detail": "경찰은 대기 시간 동안 도둑을 체포할 수 없습니다.",
                                                        "instance": "/api/games/1/system/arrest"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "대상이 도둑이 아님",
                                            value = """
                                                    {
                                                        "title": "도둑만 체포 가능",
                                                        "status": 400,
                                                        "detail": "도둑 팀만 체포될 수 있습니다.",
                                                        "instance": "/api/games/1/system/arrest"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "참가자 게임 불일치",
                                            value = """
                                                    {
                                                        "title": "참가자 게임 불일치",
                                                        "status": 400,
                                                        "detail": "경찰과 도둑이 서로 다른 게임에 참여하고 있습니다.",
                                                        "instance": "/api/games/1/system/arrest"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "이미 체포된 도둑",
                                            value = """
                                                    {
                                                        "title": "이미 체포됨",
                                                        "status": 400,
                                                        "detail": "이미 수감된 도둑입니다.",
                                                        "instance": "/api/games/1/system/arrest"
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
                                                "instance": "/api/games/1/system/arrest"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "참가자 정보 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "참가자를 찾을 수 없음",
                                    value = """
                                            {
                                                "title": "참가자를 찾을 수 없음",
                                                "status": 404,
                                                "detail": "해당 게임에 참가하지 않은 사용자입니다.",
                                                "instance": "/api/games/1/system/arrest"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ArrestResponse> arrestRobber(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "체포 요청 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ArrestRequest.class),
                            examples = @ExampleObject(
                                    name = "도둑 체포 요청",
                                    value = """
                                            {
                                                "robberParticipantId": 102
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid ArrestRequest request
    );

    @Operation(
            summary = "도둑 탈옥",
            description = """
                    수감된 도둑이 탈옥합니다.

                    - 도둑만 요청 가능
                    - 수감된 상태(JAILED)에서만 탈옥 가능
                    """,
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "탈옥 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "게임 진행 중 아님",
                                            value = """
                                                    {
                                                        "title": "게임 진행 중 아님",
                                                        "status": 400,
                                                        "detail": "게임이 진행 중인 상태가 아닙니다.",
                                                        "instance": "/api/games/1/system/escape"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "도둑이 아님",
                                            value = """
                                                    {
                                                        "title": "도둑만 탈옥 가능",
                                                        "status": 400,
                                                        "detail": "도둑 팀만 탈옥할 수 있습니다.",
                                                        "instance": "/api/games/1/system/escape"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "수감되지 않음",
                                            value = """
                                                    {
                                                        "title": "수감되지 않음",
                                                        "status": 400,
                                                        "detail": "수감된 상태에서만 탈옥할 수 있습니다.",
                                                        "instance": "/api/games/1/system/escape"
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
                                                "instance": "/api/games/1/system/escape"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "참가자 정보 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "참가자를 찾을 수 없음",
                                    value = """
                                            {
                                                "title": "참가자를 찾을 수 없음",
                                                "status": 404,
                                                "detail": "해당 게임에 참가하지 않은 사용자입니다.",
                                                "instance": "/api/games/1/system/escape"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<Void> escapeFromPrison(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );
}
