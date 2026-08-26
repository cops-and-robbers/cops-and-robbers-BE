package com.team.cops_and_robbers.user.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.user.exception.UserException;
import com.team.cops_and_robbers.user.presentation.dto.request.AgreementRequest;
import com.team.cops_and_robbers.user.presentation.dto.request.GamePushAgreementRequest;
import com.team.cops_and_robbers.user.presentation.dto.request.NicknameUpdateRequest;
import com.team.cops_and_robbers.user.presentation.dto.request.ProfileIconUpdateRequest;
import com.team.cops_and_robbers.user.presentation.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User", description = "사용자 정보 및 프로필 관리 API")
public interface UserControllerDocs {

    @Operation(summary = "내 정보 조회",
            description = "로그인한 사용자의 상세 정보를 조회합니다. (일단은 테스트용)"
    )
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<MyPageResponse> myPage(@Parameter(hidden = true) LoginUser loginUser);

    @Operation(summary = "참여 중인 게임 정보 조회",
            description = "로그인한 사용자가 현재 참여 중인 게임 정보를 조회합니다. isParticipating으로 참여 여부를 확인할 수 있으며, 참여 중인 게임이 없으면 participation이 null로 반환됩니다. 생성 후 24시간이 지난 대기 중(WAITING) 로비는 자동으로 삭제되며 isParticipating=false로 응답합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공 (참여 중인 게임이 없으면 participationInfo null 반환)")
    })
    ResponseEntity<UserGameInfoResponse> getUserGameInfo(@Parameter(hidden = true) LoginUser loginUser);

    @Operation(summary = "닉네임 중복 확인",
            description = "닉네임 변경 전, 해당 닉네임이 이미 사용 중인지 확인합니다. (쿼리 파라미터로 요청)"
    )
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "확인 완료")
    })
    ResponseEntity<NicknameCheckResponse> checkDuplicate(
            @Parameter(description = "확인할 닉네임", required = true, example = "민첩한괴도5308")
            @RequestParam String nickname
    );

    @Operation(summary = "닉네임 변경",
            description = "로그인한 사용자의 닉네임을 변경합니다. (최대 10자, 중복 불가)"
    )
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND", "DUPLICATED_NICKNAME"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "변경 성공 (응답 본문 없음), 현재 유저 본인의 닉네임으로 변경 요청했을 시 또한 204 응답")
    })
    ResponseEntity<Void> updateNickname(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid NicknameUpdateRequest request
    );


    @Operation(summary = "회원탈퇴",
            description = "로그인한 사용자의 사용자 정보를 삭제합니다."
    )
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND", "CANNOT_WITHDRAW"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 성공")
    })
    ResponseEntity<DeleteAccountResponse> deleteAccount(@Parameter(hidden = true) LoginUser loginUser);


    @Operation(summary = "약관 동의 현황 조회",
            description = "로그인한 사용자의 약관 동의 현황을 조회합니다."
    )
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<AgreementResponse> getAgreements(@Parameter(hidden = true) LoginUser loginUser);


    @Operation(summary = "약관 동의",
            description = "이용약관, 개인정보처리방침, 위치정보 이용약관(필수), 마케팅 수신 동의(선택)를 저장합니다. 필수 동의 약관들은 모두 true 여야 합니다."
    )
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND", "REQUIRED_TERMS_NOT_AGREED"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "약관 동의 저장 성공 (응답 본문 없음)")
    })
    ResponseEntity<Void> updateTerms(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid AgreementRequest request
    );

    @Operation(summary = "게임 푸시 알림 수신 동의 여부 조회",
            description = "로그인한 사용자의 게임 푸시 알림 수신 동의 여부를 조회합니다."
    )
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<GamePushAgreementResponse> getGamePushAgreement(@Parameter(hidden = true) LoginUser loginUser);

    @Operation(summary = "게임 푸시 알림 수신 동의 여부 업데이트",
            description = "로그인한 사용자의 게임 푸시 알림 수신 동의 여부를 업데이트합니다."
    )
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "업데이트 성공 (응답 본문 없음)")
    })
    ResponseEntity<Void> updateGamePushAgreement(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid GamePushAgreementRequest request
    );

    @Operation(summary = "프로필 아이콘 변경",
            description = "로그인한 사용자의 프로필 아이콘 번호를 변경합니다. 아이콘 번호는 앱에 내장된 SVG 에셋 번호와 1:1로 대응하며, 서버는 값의 범위를 검증하지 않습니다."
    )
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "변경 성공 (응답 본문 없음)")
    })
    ResponseEntity<Void> updateProfileIcon(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid ProfileIconUpdateRequest request
    );
}
