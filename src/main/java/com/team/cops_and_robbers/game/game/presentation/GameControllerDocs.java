package com.team.cops_and_robbers.game.game.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameAreaRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameCreateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameAreaUpdateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameCreateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameInfoResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameSettingsUpdateResponse;
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

@Tag(name = "Game", description = "게임 방 생성 및 관리 API")
public interface GameControllerDocs {

    @Operation(summary = "게임 방 생성",
            description = "새로운 게임 방을 생성하고 초대 코드를 발급받습니다. 게임 영역(플레이그라운드, 감옥)과 게임 규칙을 설정할 수 있습니다. 방을 생성한 사용자는 자동으로 방장이 됩니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "게임 방 생성 성공 (리소스 생성됨)",
                    content = @Content(
                            schema = @Schema(implementation = GameCreateResponse.class),
                            examples = @ExampleObject(
                                    name = "게임 생성 성공 예시",
                                    value = """
                                            {
                                                "gameId": 1,
                                                "inviteCode": "ABC123",
                                                "status": "WAITING",
                                                "roundDurationMinutes": 30,
                                                "locationRevealIntervalMinutes": 5,
                                                "policeWaitMinutes": 3,
                                                "maxParticipants": 10,
                                                "createdAt": "2026-01-16T01:25:37.543066"
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
                                            name = "필수 필드 누락",
                                            value = """
                                                    {
                                                        "title": "유효하지 않은 입력값",
                                                        "status": 400,
                                                        "detail": "area: 영역 설정은 필수입니다.",
                                                        "instance": "/api/games"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "감옥이 플레이그라운드 밖에 위치한 경우",
                                            value = """
                                                    {
                                                        "title": "영역 설정 오류",
                                                        "status": 400,
                                                        "detail": "감옥 영역이 플레이그라운드 영역 내에 포함되어야 합니다.",
                                                        "instance": "/api/games"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "위치 공개 주기가 라운드 시간보다 긴 경우",
                                            value = """
                                                    {
                                                        "title": "게임 설정 오류",
                                                        "status": 400,
                                                        "detail": "위치 공개 주기는 라운드 시간보다 짧아야 합니다.",
                                                        "instance": "/api/games"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "JSON 형식 오류",
                                            value = """
                                                    {
                                                        "title": "잘못된 요청 본문",
                                                        "status": 400,
                                                        "detail": "요청 본문의 형식이 잘못되었습니다.",
                                                        "instance": "/api/games"
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
                                                "instance": "/api/games"
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
                                                "instance": "/api/games"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<GameCreateResponse> createGame(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "게임 생성 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GameCreateRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "정상 요청 예시",
                                            value = """
                                                    {
                                                        "area": {
                                                            "playgroundCenter": {
                                                                "latitude": 37.5665,
                                                                "longitude": 126.978
                                                            },
                                                            "playgroundRadiusInMeters": 1000,
                                                            "jailCenter": {
                                                                "latitude": 37.5665,
                                                                "longitude": 126.978
                                                            },
                                                            "jailRadiusInMeters": 100
                                                        },
                                                        "settings": {
                                                            "roundDurationMinutes": 30,
                                                            "locationRevealIntervalMinutes": 5,
                                                            "policeWaitMinutes": 3,
                                                            "maxParticipants": 10
                                                        }
                                                    }
                                                    """
                                    )
                            }
                    )
            ) @RequestBody @Valid GameCreateRequest request
    );

    @Operation(
            summary = "게임 기본 설정 조회",
            description = """
                    게임의 기본 설정 정보를 조회합니다.

                    - 대기 중(WAITING) 또는 진행 중(IN_PROGRESS) 상태에서만 조회 가능
                    - 해당 게임의 참가자만 조회 가능
                    """,
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게임 설정 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = GameInfoResponse.class),
                            examples = @ExampleObject(
                                    name = "게임 설정 조회 성공 예시",
                                    value = """
                                            {
                                                "roundDurationMinutes": 30,
                                                "locationRevealIntervalMinutes": 5,
                                                "policeWaitMinutes": 3,
                                                "maxParticipants": 10
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "비활성 게임",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "비활성 게임",
                                    value = """
                                            {
                                                "title": "비활성 게임",
                                                "status": 400,
                                                "detail": "대기 중이거나 진행 중인 게임에서만 조회할 수 있습니다.",
                                                "instance": "/api/games/1"
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
                                                "instance": "/api/games/1"
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
                                                        "instance": "/api/games/999"
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
                                                        "instance": "/api/games/1"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<GameInfoResponse> getGameInfo(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );

    @Operation(summary = "게임 영역 수정 (방장 전용)",
            description = """
                    대기실에서 플레이그라운드·감옥 영역을 수정합니다.

                    **제약 조건**
                    - 방장만 호출 가능
                    - 게임이 WAITING 상태일 때만 가능
                    - 플레이그라운드와 감옥을 반드시 세트로 전송 (부분 수정 불가)
                    - 감옥은 플레이그라운드 내부에 완전히 포함되어야 함
                    - 감옥 반경 < 플레이그라운드 반경

                    **WebSocket**
                    성공 시 대기실 구독자 전체에게 `AREA_UPDATED` 이벤트가 브로드캐스트됩니다.
                    """,
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "영역 수정 성공",
                    content = @Content(
                            schema = @Schema(implementation = GameAreaUpdateResponse.class),
                            examples = @ExampleObject(
                                    name = "수정 성공 예시",
                                    value = """
                                            {
                                                "playgroundCenter": {
                                                    "latitude": 37.5665,
                                                    "longitude": 126.9780
                                                },
                                                "playgroundRadiusInMeters": 1000,
                                                "jailCenter": {
                                                    "latitude": 37.5670,
                                                    "longitude": 126.9785
                                                },
                                                "jailRadiusInMeters": 100
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
                                            name = "감옥이 플레이그라운드 밖에 위치",
                                            value = """
                                                    {
                                                        "title": "감옥 영역 벗어남",
                                                        "status": 400,
                                                        "detail": "감옥은 운동장 내부에 완전히 포함되어야 합니다.",
                                                        "instance": "/api/games/1/area"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "게임이 대기 중이 아닌 경우",
                                            value = """
                                                    {
                                                        "title": "대기 중인 게임이 아님",
                                                        "status": 400,
                                                        "detail": "대기 중인 게임에서만 설정을 변경할 수 있습니다.",
                                                        "instance": "/api/games/1/area"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "title": "호스트 권한 필요",
                                                "status": 403,
                                                "detail": "방장만 게임을 시작할 수 있습니다.",
                                                "instance": "/api/games/1/area"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<GameAreaUpdateResponse> updateGameArea(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 전체 영역 정보 (플레이그라운드 + 감옥 세트로 전송)",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GameAreaRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "playgroundCenter": {
                                                    "latitude": 37.5665,
                                                    "longitude": 126.9780
                                                },
                                                "playgroundRadiusInMeters": 1000,
                                                "jailCenter": {
                                                    "latitude": 37.5670,
                                                    "longitude": 126.9785
                                                },
                                                "jailRadiusInMeters": 100
                                            }
                                            """
                            )
                    )
            ) @RequestBody @Valid GameAreaRequest request
    );

    @Operation(summary = "게임 설정 수정 (방장 전용)",
            description = """
                    대기실에서 게임 규칙을 수정합니다.

                    **제약 조건**
                    - 방장만 호출 가능
                    - 게임이 WAITING 상태일 때만 가능
                    - 모든 설정 필드를 세트로 전송 (부분 수정 불가)
                    - 위치 공개 주기 < 라운드 시간
                    - 경찰 대기 시간 < 라운드 시간

                    **WebSocket**
                    성공 시 대기실 구독자 전체에게 `SETTINGS_UPDATED` 이벤트가 브로드캐스트됩니다.
                    """,
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "설정 수정 성공",
                    content = @Content(
                            schema = @Schema(implementation = GameSettingsUpdateResponse.class),
                            examples = @ExampleObject(
                                    name = "수정 성공 예시",
                                    value = """
                                            {
                                                "roundDurationMinutes": 60,
                                                "locationRevealIntervalMinutes": 10,
                                                "policeWaitMinutes": 5,
                                                "maxParticipants": 20
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
                                            name = "위치 공개 주기가 라운드 시간 이상인 경우",
                                            value = """
                                                    {
                                                        "title": "유효하지 않은 위치 공개 주기",
                                                        "status": 400,
                                                        "detail": "위치 공개 주기는 라운드 시간보다 짧아야 합니다.",
                                                        "instance": "/api/games/1/settings"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "게임이 대기 중이 아닌 경우",
                                            value = """
                                                    {
                                                        "title": "대기 중인 게임이 아님",
                                                        "status": 400,
                                                        "detail": "대기 중인 게임에서만 설정을 변경할 수 있습니다.",
                                                        "instance": "/api/games/1/settings"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "title": "호스트 권한 필요",
                                                "status": 403,
                                                "detail": "방장만 게임을 시작할 수 있습니다.",
                                                "instance": "/api/games/1/settings"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<GameSettingsUpdateResponse> updateGameSettings(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 전체 게임 설정 (모든 필드 필수)",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GameSettingsRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "roundDurationMinutes": 60,
                                                "locationRevealIntervalMinutes": 10,
                                                "policeWaitMinutes": 5,
                                                "maxParticipants": 20
                                            }
                                            """
                            )
                    )
            ) @RequestBody @Valid GameSettingsRequest request
    );
}
