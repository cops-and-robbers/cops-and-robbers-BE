package com.team.cops_and_robbers.game.game.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameCreateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameCreateResponse;
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
}
