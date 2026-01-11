package com.team.cops_and_robbers.user.application;

import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserInfo(Long loginUserId) {
        return userRepository.getByUserId(loginUserId);
    }



}
