package com.team.cops_and_robbers.auth.presentation;

import com.team.cops_and_robbers.auth.presentation.dto.request.LoginRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.ReissueRequest;
import com.team.cops_and_robbers.auth.presentation.dto.response.LoginResponse;
import com.team.cops_and_robbers.auth.presentation.dto.response.ReissueResponse;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
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
            description = "카카오 소셜 로그인을 통해 서비스에 로그인합니다. 신규 회원인 경우 자동으로 회원가입이 진행되며, Access Token과 Refresh Token이 발급됩니다."
    )
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
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "201", description = "신규 회원 가입 및 로그인 성공 (회원 리소스 생성)",
                    content = @Content(
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(
                                    name = "신규 회원 가입 성공 예시",
                                    value = """
                                            {
                                                "userId": 2,
                                                "nickname": "집요한괴도4053",
                                                "tokens": {
                                                    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyIiwiaWF0IjoxNzY4NDk1MDEwLCJleHAiOjE3Njg0OTg2MTB9...",
                                                    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyIiwiaWF0IjoxNzY4NDk1MDEwLCJleHAiOjE3Njk3MDQ2MTB9..."
                                                }
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
                                            name = "필수 요청 필드가 누락된 경우",
                                            value = """
                                                    {
                                                        "title": "유효하지 않은 입력값",
                                                        "status": 400,
                                                        "detail": "idToken: 소셜 인증 토큰(ID Token)은 필수입니다.",
                                                        "instance": "/api/auth/login"
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
                                                        "instance": "/api/auth/login"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "소셜 ID Token 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "유효하지 않은 ID Token",
                                    value = """
                                            {
                                                "title": "소셜 로그인 실패",
                                                "status": 401,
                                                "detail": "유효하지 않은 소셜 인증 토큰입니다.",
                                                "instance": "/api/auth/login"
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
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Refresh Token 누락",
                                    value = """
                                            {
                                                "title": "유효하지 않은 입력값",
                                                "status": 400,
                                                "detail": "refreshToken: Refresh Token은 필수입니다.",
                                                "instance": "/api/auth/reissue"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Refresh Token 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "유효하지 않은 Refresh Token",
                                    value = """
                                            {
                                                "title": "토큰 재발급 실패",
                                                "status": 401,
                                                "detail": "유효하지 않거나 만료된 Refresh Token입니다.",
                                                "instance": "/api/auth/reissue"
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
}
