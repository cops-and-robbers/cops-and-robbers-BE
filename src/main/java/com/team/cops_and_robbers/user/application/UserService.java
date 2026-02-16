package com.team.cops_and_robbers.user.application;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.application.dto.command.NicknameUpdateCommand;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GameParticipantRepository gameParticipantRepository;

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

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.getByUserId(userId);
        if (gameParticipantRepository.existsActiveGameByUserId(user.getId())) {
            throw new ApplicationException(UserException.CANNOT_WITHDRAW);
        }
        userRepository.delete(user);
        refreshTokenRepository.delete(user.getId());

        try {
            firebaseAuth.deleteUser(user.getSocialId());
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                log.warn("[Firebase] Firebase user already deleted: userId={}, socialId={}", userId, user.getSocialId());
                return;
            }
            throw new InfrastructureException(AuthException.FIREBASE_SERVER_ERROR);
        }
    }
}
