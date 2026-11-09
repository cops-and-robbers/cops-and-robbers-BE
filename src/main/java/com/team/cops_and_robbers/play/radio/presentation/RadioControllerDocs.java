package com.team.cops_and_robbers.play.radio.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.play.radio.exception.RadioException;
import com.team.cops_and_robbers.play.radio.presentation.dto.response.RadioTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Radio", description = "경찰 팀 무전(음성 대화) API")
public interface RadioControllerDocs {

    @Operation(
            summary = "무전 접속 토큰 발급",
            description = """
                    경찰이 무전 서버에 접속할 토큰을 발급받습니다.

                    - 경찰만 요청 가능
                    - 경찰 대기 시간이 끝난 뒤에만 사용 가능
                    - 이벤트 게임에서는 사용 불가
                    - 토큰 수명은 2분입니다. 수명의 70~90% 지점에서 다시 호출해 재발급받습니다
                    - 재발급 때마다 자격을 다시 검증하므로 게임이 끝났거나 나갔으면 거절됩니다
                    - 음성은 서버를 거치지 않고 무전 서버와 앱 사이에서만 흐릅니다
                    """
    )
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_IN_PROGRESS", "GAME_NOT_FOUND"})
    @ApiErrorCode(value = GameParticipantException.class, codes = {"PARTICIPANT_NOT_FOUND"})
    @ApiErrorCode(value = RadioException.class, codes = {
            "RADIO_NOT_ALLOWED_IN_EVENT_GAME", "ONLY_POLICE_CAN_USE_RADIO", "RADIO_POLICE_WAITING_TIME"
    })
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 발급 성공")
    })
    ResponseEntity<RadioTokenResponse> issueToken(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게임 ID", required = true, example = "1") @PathVariable Long gameId
    );
}
