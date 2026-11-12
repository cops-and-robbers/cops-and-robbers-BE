package com.team.cops_and_robbers.auth.presentation;

import com.team.cops_and_robbers.auth.application.AuthService;
import com.team.cops_and_robbers.auth.application.dto.result.AdminLoginResult;
import com.team.cops_and_robbers.auth.application.dto.result.LoginResult;
import com.team.cops_and_robbers.auth.application.dto.result.WithdrawalPreviewResult;
import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.auth.presentation.dto.request.AdminLoginRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.LoginRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.ReissueRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.LogoutRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.WithdrawalRequest;
import com.team.cops_and_robbers.auth.presentation.dto.response.AdminLoginResponse;
import com.team.cops_and_robbers.auth.presentation.dto.response.LoginResponse;
import com.team.cops_and_robbers.auth.presentation.dto.response.ReissueResponse;
import com.team.cops_and_robbers.auth.presentation.dto.response.WithdrawalPreviewResponse;
import com.team.cops_and_robbers.user.presentation.dto.response.DeleteAccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    /**
     * 1. 소셜 로그인을 진행합니다.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResult loginResult = authService.login(request.toCommand());
        LoginResponse response = LoginResponse.from(loginResult);

        if (loginResult.isNewUser()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Refresh token 으로 access token 을 재발급합니다.
     */
    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(@RequestBody @Valid ReissueRequest request) {
        Tokens tokens = authService.reissueTokens(request.refreshToken());
        return ResponseEntity.ok(ReissueResponse.from(tokens));
    }

    /**
     * 3. 로그아웃을 진행합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * 4. 어드민 웹 로그인을 진행합니다.
     */
    @PostMapping("/admin/login")
    public ResponseEntity<AdminLoginResponse> adminLogin(@RequestBody @Valid AdminLoginRequest request) {
        AdminLoginResult result = authService.adminLogin(request.toCommand());
        AdminLoginResponse response = AdminLoginResponse.from(result);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. 어드민 웹 로그아웃을 진행합니다.
     */
    @PostMapping("/admin/logout")
    public ResponseEntity<Void> adminLogout(@RequestBody @Valid LogoutRequest request) {
        authService.adminLogout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * 6. 웹 회원 탈퇴 전 계정을 확인합니다.
     */
    @PostMapping("/withdrawal/preview")
    public ResponseEntity<WithdrawalPreviewResponse> previewWithdrawal(@RequestBody @Valid WithdrawalRequest request) {
        WithdrawalPreviewResult result = authService.previewWithdrawal(request.toCommand());
        return ResponseEntity.ok(WithdrawalPreviewResponse.from(result));
    }

    /**
     * 7. 웹에서 회원 탈퇴를 진행합니다.
     */
    @PostMapping("/withdrawal")
    public ResponseEntity<DeleteAccountResponse> withdraw(@RequestBody @Valid WithdrawalRequest request) {
        authService.withdraw(request.toCommand());
        return ResponseEntity.ok(DeleteAccountResponse.from());
    }

}
