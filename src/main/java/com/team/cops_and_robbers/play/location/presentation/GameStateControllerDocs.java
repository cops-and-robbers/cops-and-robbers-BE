package com.team.cops_and_robbers.play.location.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.play.location.presentation.dto.GameStateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Location", description = "게임 위치 API")
public interface GameStateControllerDocs {

    @Operation(
            summary = "게임 현재 상태 조회",
            description = """
                    현재 게임 상태를 조회합니다. 소켓 재연결 시 게임 화면(상태) 복원에 활용됩니다.
                    
                    - 가장 최근 도둑 위치 공개 시점의 위치를 기준으로 목록을 응답합니다.

                    **응답 내용**
                    - `robberLocations`: 마지막 위치 공개 주기에 찍힌 생존 도둑들의 위치
                    - `participants`: 전체 참가자의 팀·상태 현황

                    **robberLocations 반환 조건**

                    | 게임 단계 | robberLocations |
                    |---|---|
                    | 경찰 대기 시간 중 (POLICE_WAITING) | 빈 배열 |
                    | 경찰 출동 후 첫 위치 공개 전 | 빈 배열 |
                    | 첫 위치 공개 이후 | 생존 도둑 위치 목록 (JAILED 제외) |

                    > 경찰 대기 시간 중에는 경찰·도둑 구분 없이 모두 빈 배열이 반환됩니다.
                    """
    )
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_FOUND", "GAME_NOT_IN_PROGRESS"})
    @ApiErrorCode(value = GameParticipantException.class, codes = {"PARTICIPANT_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게임 상태 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = GameStateResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "경찰 대기 단계 (POLICE_WAITING)",
                                            summary = "경찰이 아직 출동하지 않은 상태 — 누구도 도둑 위치를 볼 수 없음",
                                            value = """
                                                    {
                                                      "robberLocations": [],
                                                      "participants": [
                                                        {"participantId": 1, "nickname": "빠른경찰1", "team": "POLICE", "status": "POLICE_WAITING"},
                                                        {"participantId": 2, "nickname": "민첩한괴도1", "team": "ROBBER", "status": "ALIVE"},
                                                        {"participantId": 3, "nickname": "교활한괴도2", "team": "ROBBER", "status": "ALIVE"}
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "경찰 출동 후 첫 위치 공개 전",
                                            summary = "경찰 출동 완료, 아직 위치 공개 주기가 도래하지 않아 스냅샷 없음",
                                            value = """
                                                    {
                                                      "robberLocations": [],
                                                      "participants": [
                                                        {"participantId": 1, "nickname": "빠른경찰1", "team": "POLICE", "status": "ALIVE"},
                                                        {"participantId": 2, "nickname": "민첩한괴도1", "team": "ROBBER", "status": "ALIVE"},
                                                        {"participantId": 3, "nickname": "교활한괴도2", "team": "ROBBER", "status": "ALIVE"}
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "위치 공개 이후 (일부 도둑 체포)",
                                            summary = "최소 1회 위치 공개 완료, JAILED 도둑은 위치 목록에서 제외되고 participants에만 포함",
                                            value = """
                                                    {
                                                      "robberLocations": [
                                                        {"participantId": 2, "nickname": "민첩한괴도1", "latitude": 37.5665, "longitude": 126.9780}
                                                      ],
                                                      "participants": [
                                                        {"participantId": 1, "nickname": "빠른경찰1", "team": "POLICE", "status": "ALIVE"},
                                                        {"participantId": 2, "nickname": "민첩한괴도1", "team": "ROBBER", "status": "ALIVE"},
                                                        {"participantId": 3, "nickname": "교활한괴도2", "team": "ROBBER", "status": "JAILED"}
                                                      ]
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ResponseEntity<GameStateResponse> getGameState(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );
}
