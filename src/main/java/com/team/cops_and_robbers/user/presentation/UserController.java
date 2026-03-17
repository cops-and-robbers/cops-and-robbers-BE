package com.team.cops_and_robbers.user.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.user.application.UserService;
import com.team.cops_and_robbers.user.application.dto.command.NicknameUpdateCommand;
import com.team.cops_and_robbers.user.application.dto.result.UserGameInfoResult;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.presentation.dto.request.NicknameUpdateRequest;
import com.team.cops_and_robbers.user.presentation.dto.response.DeleteAccountResponse;
import com.team.cops_and_robbers.user.presentation.dto.response.MyPageResponse;
import com.team.cops_and_robbers.user.presentation.dto.response.NicknameCheckResponse;
import com.team.cops_and_robbers.user.presentation.dto.response.UserGameInfoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;

    /**
     * 1. 로그인한 사용자의 정보를 조회합니다.
     */
    @GetMapping("/me")
    public ResponseEntity<MyPageResponse> myPage(@AuthUser LoginUser loginUser) {
        User user = userService.getUserInfo(loginUser.userId());
        return ResponseEntity.ok().body(MyPageResponse.from(user));
    }

    /**
     * 2. 참여 중인 게임 정보를 조회합니다.
     */
    @GetMapping("/me/game")
    public ResponseEntity<UserGameInfoResponse> getUserGameInfo(@AuthUser LoginUser loginUser) {
        Optional<UserGameInfoResult> result = userService.getUserGameInfo(loginUser.userId());
        UserGameInfoResponse response = result.map(UserGameInfoResponse::from).orElse(null);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. 닉네임의 중복 여부를 확인합니다.
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<NicknameCheckResponse> checkDuplicate(@RequestParam String nickname) {
        boolean isDuplicated = userService.isDuplicate(nickname);
        return ResponseEntity.ok().body(NicknameCheckResponse.from(isDuplicated));
    }

    /**
     * 4. 사용자 닉네임을 변경합니다.
     */
    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthUser LoginUser loginUser,
            @RequestBody @Valid NicknameUpdateRequest request
    ) {
        NicknameUpdateCommand command = NicknameUpdateCommand.of(loginUser.userId(), request.nickname());
        userService.updateNickname(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * 5. 사용자 정보를 삭제합니다. (회원 탈퇴)
     */
    @DeleteMapping("/me")
    public ResponseEntity<DeleteAccountResponse> deleteAccount(@AuthUser LoginUser loginUser) {
        userService.deleteAccount(loginUser.userId());
        return ResponseEntity.ok(DeleteAccountResponse.from());
    }
}
