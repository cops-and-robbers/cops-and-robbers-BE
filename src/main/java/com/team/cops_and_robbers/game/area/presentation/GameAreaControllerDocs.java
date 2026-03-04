package com.team.cops_and_robbers.game.area.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.game.area.presentation.dto.response.GameAreaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Game Area", description = "게임 맵 정보 조회 API")
public interface GameAreaControllerDocs {

    @Operation(
            summary = "게임 맵 정보 조회",
            description = """
                    플레이그라운드 및 감옥 영역 정보를 조회합니다.

                    - 게임이 대기 중(WAITING) 또는 진행 중(IN_PROGRESS) 상태에서만 조회 가능
                    - 해당 게임의 참가자만 조회 가능
                    """,
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "맵 정보 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = GameAreaResponse.class),
                            examples = @ExampleObject(
                                    name = "맵 정보 조회 성공 예시",
                                    value = """
                                            {
                                                "playgroundCenter": {
                                                    "latitude": 37.5665,
                                                    "longitude": 126.978
                                                },
                                                "playgroundRadiusInMeters": 1000,
                                                "jailCenter": {
                                                    "latitude": 37.566,
                                                    "longitude": 126.977
                                                },
                                                "jailRadiusInMeters": 100
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
                                                "instance": "/api/games/1/area"
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
                                                "instance": "/api/games/1/area"
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
                                                        "instance": "/api/games/999/area"
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
                                                        "instance": "/api/games/1/area"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<GameAreaResponse> getGameArea(
            @Parameter(hidden = true) @AuthUser LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );
}