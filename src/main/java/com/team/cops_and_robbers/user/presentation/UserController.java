package com.team.cops_and_robbers.user.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.user.application.UserService;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.presentation.dto.response.MyPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 1. 로그인한 사용자의 정보를 조회합니다.
     */
    @GetMapping("/me")
    public ResponseEntity<MyPageResponse> myPage(@AuthUser LoginUser loginUser) {
        User user = userService.getUserInfo(loginUser.userId());
        return ResponseEntity.ok().body(MyPageResponse.from(user));
    }

}
