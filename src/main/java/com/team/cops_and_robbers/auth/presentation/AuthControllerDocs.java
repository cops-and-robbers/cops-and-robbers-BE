package com.team.cops_and_robbers.auth.presentation;

import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.presentation.dto.request.LoginRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.LogoutRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.ReissueRequest;
import com.team.cops_and_robbers.auth.presentation.dto.response.LoginResponse;
import com.team.cops_and_robbers.auth.presentation.dto.response.ReissueResponse;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "소셜 로그인 및 토큰 관리 API")
public interface AuthControllerDocs {

    @Operation(summary = "소셜 로그인",
            description = "소셜 로그인을 통해 서비스에 로그인합니다. 신규 회원인 경우 자동으로 회원가입이 진행되며, Access Token과 Refresh Token이 발급됩니다."
    )
    @ApiErrorCode(value = AuthException.class, codes = {"INVALID_FIREBASE_TOKEN", "EXPIRED_FIREBASE_TOKEN"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기존 회원 로그인 성공",
                    content = @Content(
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(
                                    name = "기존 회원 로그인 성공 예시",
                                    value = """
                                            {
                                                "userId": 1,
                                                "nickname": "민첩한괴도5308",
                                                "tokens": {
                                                    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzY4NDk1MDA1LCJleHAiOjE3Njg0OTg2MDV9...",
                                                    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzY4NDk1MDA1LCJleHAiOjE3Njk3MDQ2MDV9..."
                                                },
                                                "isNewUser": false,
                                                "requiresAgreement": false
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "201", description = "신규 회원 가입 및 로그인 성공 (회원 리소스 생성)",
                    content = @Content(
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(
                                    name = "신규 회원 가입 성공 예시 (신규 회원은 항상 필수 이용 약관에 동의를 하지 않은 상태를 갖는다)",
                                    value = """
                                            {
                                                "userId": 2,
                                                "nickname": "집요한괴도4053",
                                                "tokens": {
                                                    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyIiwiaWF0IjoxNzY4NDk1MDEwLCJleHAiOjE3Njg0OTg2MTB9...",
                                                    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyIiwiaWF0IjoxNzY4NDk1MDEwLCJleHAiOjE3Njk3MDQ2MTB9..."
                                                },
                                                "isNewUser": true,
                                                "requiresAgreement": true
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "소셜 로그인 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "정상 요청 예시",
                                            value = """
                                                    {
                                                        "socialPlatform": "KAKAO",
                                                        "idToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                        "fcmToken": "fcm-device-token-here",
                                                        "deviceType": "IOS",
                                                        "deviceId": "unique-device-id-123"
                                                    }
                                                    """
                                    )
                            }
                    )
            ) @RequestBody @Valid LoginRequest request
    );

    @Operation(summary = "토큰 재발급",
            description = "만료된 Access Token을 Refresh Token을 사용하여 재발급받습니다."
    )
    @ApiErrorCode(value = AuthException.class, codes = {"REFRESH_TOKEN_EXPIRED", "INVALID_TOKEN"})
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(
                            schema = @Schema(implementation = ReissueResponse.class),
                            examples = @ExampleObject(
                                    name = "토큰 재발급 성공 예시",
                                    value = """
                                            {
                                                "tokens": {
                                                    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIx.....",
                                                    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIx....."
                                                }
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ReissueResponse> reissue(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "토큰 재발급 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ReissueRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "정상 요청 예시",
                                            value = """
                                                    {
                                                        "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzY4NDk1MDA1LCJleHAiOjE3Njk3MDQ2MDV9..."
                                                    }
                                                    """
                                    )
                            }
                    )
            ) @RequestBody @Valid ReissueRequest request
    );


    @Operation(summary = "로그아웃",
            description = "로그아웃합니다. (리프레시 토큰 삭제 + 유저 디바이스 정보 삭제)"
    )
    @ApiErrorCode(value = CommonException.class, codes = {"INVALID_INPUT_VALUE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "로그아웃 성공 (항상 204 응답)"),
    })
    ResponseEntity<Void> logout(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "로그아웃 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LogoutRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "정상 요청 예시",
                                            value = """
                                                    {
                                                        "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzY4NDk1MDA1LCJleHAiOjE3Njk3MDQ2MDV9..."
                                                    }
                                                    """
                                    )
                            }
                    )
            ) @RequestBody @Valid LogoutRequest request
    );
}
