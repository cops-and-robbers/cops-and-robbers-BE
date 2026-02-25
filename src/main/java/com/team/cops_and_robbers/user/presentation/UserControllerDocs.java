package com.team.cops_and_robbers.user.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.user.presentation.dto.request.NicknameUpdateRequest;
import com.team.cops_and_robbers.user.presentation.dto.response.DeleteAccountResponse;
import com.team.cops_and_robbers.user.presentation.dto.response.MyPageResponse;
import com.team.cops_and_robbers.user.presentation.dto.response.NicknameCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = MyPageResponse.class),
                            examples = @ExampleObject(
                                    name = "내 정보 조회 예시",
                                    value = """
                                            {
                                               "userId": 7,
                                               "nickname": "민첩한괴도5308",
                                               "socialPlatform": "KAKAO",
                                               "allowGamePush": true,
                                               "allowMarketingPush": false
                                             }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 없음/만료)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "인증 실패",
                                    value = """
                                            {
                                                "title": "인증 실패",
                                                "status": 401,
                                                "detail": "유효하지 않은 토큰입니다.",
                                                "instance": "/api/user/me"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<MyPageResponse> myPage(@Parameter(hidden = true) LoginUser loginUser);

    @Operation(summary = "닉네임 중복 확인",
            description = "닉네임 변경 전, 해당 닉네임이 이미 사용 중인지 확인합니다. (쿼리 파라미터로 요청)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "확인 완료",
                    content = @Content(
                            schema = @Schema(implementation = NicknameCheckResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "사용 중인 닉네임 (중복)",
                                            value = """
                                                    {
                                                       "isAvailable": false,
                                                       "message": "이미 사용 중인 닉네임입니다."
                                                     }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "사용 가능한 닉네임",
                                            value = """
                                                    {
                                                       "isAvailable": true,
                                                       "message": "사용 가능한 닉네임 입니다!"
                                                     }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "파라미터 누락",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "닉네임 파라미터 누락",
                                    value = """
                                            {
                                                "title": "잘못된 요청",
                                                "status": 400,
                                                "detail": "Required request parameter 'nickname' for method parameter type String is not present",
                                                "instance": "/api/user/check-nickname"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<NicknameCheckResponse> checkDuplicate(
            @Parameter(description = "확인할 닉네임", required = true, example = "민첩한괴도5308")
            @RequestParam String nickname
    );

    @Operation(summary = "닉네임 변경",
            description = "로그인한 사용자의 닉네임을 변경합니다. (최대 10자, 중복 불가)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "변경 성공 (응답 본문 없음), 현재 유저 본인의 닉네임으로 변경 요청했을 시 또한 204 응답"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패 (길이, 공백 등)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "길이 초과 (10자)",
                                            value = """
                                                    {
                                                        "title": "유효하지 않은 입력값",
                                                        "status": 400,
                                                        "detail": "nickname: 닉네임은 최대 10자까지 가능합니다.",
                                                        "instance": "/api/user/me/nickname"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "공백 입력",
                                            value = """
                                                    {
                                                        "title": "유효하지 않은 입력값",
                                                        "status": 400,
                                                        "detail": "nickname: 닉네임은 필수 입력 항목입니다.",
                                                        "instance": "/api/user/me/nickname"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "409", description = "닉네임 중복 예외",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "이미 존재하는 닉네임",
                                    value = """
                                            {
                                                "title": "닉네임 중복",
                                                "status": 409,
                                                "detail": "이미 사용 중인 닉네임입니다. 다른 닉네임을 선택해주세요.",
                                                "instance": "/api/user/me/nickname"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<Void> updateNickname(
            @Parameter(hidden = true) LoginUser loginUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "닉네임 변경 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = NicknameUpdateRequest.class),
                            examples = @ExampleObject(
                                    name = "정상 요청",
                                    value = """
                                            {
                                                "nickname": "날렵한경찰123"
                                            }
                                            """
                            )
                    )
            ) @RequestBody @Valid NicknameUpdateRequest request
    );


    @Operation(summary = "회원탈퇴",
            description = "로그인한 사용자의 사용자 정보를 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 성공",
                    content = @Content(
                            schema = @Schema(implementation = DeleteAccountResponse.class),
                            examples = @ExampleObject(
                                    name = "회원 탈퇴 예시",
                                    value = """
                                            {
                                               "message": "회원탈퇴가 완료되었습니다."
                                             }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 없음/만료)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "인증 실패",
                                    value = """
                                            {
                                                "title": "인증 실패",
                                                "status": 401,
                                                "detail": "유효하지 않은 토큰입니다.",
                                                "instance": "/api/user/me"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "409", description = "진행 중인 게임이 있는 경우",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "회원탈퇴 불가",
                                    value = """
                                            {
                                                "title": "회원탈퇴 불가",
                                                "status": 401,
                                                "detail": "진행 중인 게임 세션이 있어 탈퇴할 수 없습니다.",
                                                "instance": "/api/user/me"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<DeleteAccountResponse> deleteAccount(@Parameter(hidden = true) LoginUser loginUser);
}
