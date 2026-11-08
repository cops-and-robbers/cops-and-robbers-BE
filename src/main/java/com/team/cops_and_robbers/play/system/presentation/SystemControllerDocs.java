package com.team.cops_and_robbers.play.system.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.play.system.presentation.dto.request.ArrestRequest;
import com.team.cops_and_robbers.play.system.presentation.dto.response.ArrestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
                    """
    )
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_IN_PROGRESS", "GAME_NOT_FOUND"})
    @ApiErrorCode(value = GameParticipantException.class, codes = {
            "ONLY_POLICE_CAN_ARREST", "POLICE_WAITING_TIME", "ONLY_ROBBER_CAN_BE_ARRESTED",
            "PARTICIPANT_GAME_MISMATCH", "ALREADY_ARRESTED", "PARTICIPANT_NOT_FOUND"
    })
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "체포 성공")
    })
    ResponseEntity<ArrestResponse> arrestRobber(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId,
            @RequestBody @Valid ArrestRequest request
    );

    @Operation(
            summary = "도둑 강제 체포",
            description = """
                    구역 밖에 오래 머문 도둑이 스스로 체포됩니다.

                    - 구역 이탈 판정과 경고 횟수는 앱이 처리하고, 서버는 위치를 검증하지 않음
                    - 로그인한 도둑 본인이 체포 대상이므로 요청 본문 없음
                    - 이미 수감된 도둑은 체포 불가
                    - 이벤트 게임에서는 체포 불가
                    - 경찰 대기 시간 중에도 요청 가능
                    - 경찰의 체포 수는 올라가지 않고, ARREST 이벤트의 police 가 null 로 전송됨
                    """
    )
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_IN_PROGRESS", "FORCE_ARREST_NOT_ALLOWED_IN_EVENT_GAME", "GAME_NOT_FOUND"})
    @ApiErrorCode(value = GameParticipantException.class, codes = {
            "ONLY_ROBBER_CAN_BE_ARRESTED", "ALREADY_ARRESTED", "PARTICIPANT_NOT_FOUND"
    })
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "강제 체포 성공")
    })
    ResponseEntity<ArrestResponse> forceArrestRobber(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );

    @Operation(
            summary = "도둑 탈옥",
            description = """
                    수감된 도둑이 탈옥합니다.

                    - 도둑만 요청 가능
                    - 수감된 상태(JAILED)에서만 탈옥 가능
                    """
    )
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_IN_PROGRESS", "GAME_NOT_FOUND"})
    @ApiErrorCode(value = GameParticipantException.class, codes = {"NOT_JAILED", "PARTICIPANT_NOT_FOUND"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "탈옥 성공")
    })
    ResponseEntity<Void> escapeFromPrison(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );
}
