package com.team.cops_and_robbers.user.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.user.application.dto.command.NicknameUpdateCommand;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUserInfo(Long loginUserId) {
        return userRepository.getByUserId(loginUserId);
    }

    @Transactional(readOnly = true)
    public boolean isDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Transactional
    public void updateNickname(NicknameUpdateCommand command) {
        User user = userRepository.getByUserId(command.userId());

        if (user.hasSameNickname(command.nickname())) {
            return;
        }
        user.updateNickname(command.nickname());

        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(UserException.DUPLICATED_NICKNAME);
        }
    }
}
