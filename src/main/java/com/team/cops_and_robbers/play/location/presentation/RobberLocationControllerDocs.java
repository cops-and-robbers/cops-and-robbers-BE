package com.team.cops_and_robbers.play.location.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.play.location.presentation.dto.RobberLocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Location", description = "게임 위치 API")
public interface RobberLocationControllerDocs {

    @Operation(
            summary = "도둑 위치 조회",
            description = """
                    현재 게임에 참여 중인 도둑들의 마지막 위치를 조회합니다.

                    - 위치를 한 번도 전송하지 않은 도둑은 목록에 포함되지 않습니다.
                    """
    )
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_FOUND", "GAME_NOT_IN_PROGRESS"})
    @ApiErrorCode(value = GameParticipantException.class, codes = {"PARTICIPANT_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<List<RobberLocationResponse>> getRobberLocations(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );
}
