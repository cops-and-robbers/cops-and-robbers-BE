package com.team.cops_and_robbers.user.application;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.application.dto.command.NicknameUpdateCommand;
import com.team.cops_and_robbers.user.application.dto.result.UserGameInfoResult;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final long LOBBY_TTL_HOURS = 24;

    private final Clock clock;
    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    public User getUserInfo(Long loginUserId) {
        return userRepository.getByUserId(loginUserId);
    }

    @Transactional
    public Optional<UserGameInfoResult> getUserGameInfo(Long userId) {
        Optional<GameParticipant> OptionalParticipant = gameParticipantRepository.findActiveParticipantByUserId(userId);
        if (OptionalParticipant.isEmpty()) {
            return Optional.empty();
        }

        GameParticipant participant = OptionalParticipant.get();
        Game game = participant.getGame();

        if (isExpiredLobby(game)) {
            gameParticipantRepository.deleteAllByGameId(game.getId());
            gameAreaRepository.deleteByGameId(game.getId());
            gameRepository.delete(game);
            return Optional.empty();
        }

        return Optional.of(UserGameInfoResult.from(participant));
    }

    private boolean isExpiredLobby(Game game) {
        LocalDateTime lobbyExpirationTime = LocalDateTime.now(clock).minusHours(LOBBY_TTL_HOURS);

        return game.isWaiting() && game.getCreatedAt().isBefore(lobbyExpirationTime);
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
        userDeviceRepository.deleteByUserId(userId);
        userRepository.deleteUserByIdDirectly(user.getId());
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
