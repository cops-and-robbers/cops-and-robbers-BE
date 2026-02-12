package com.team.cops_and_robbers;

import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GameRepository gameRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("이미 데이터가 존재하여 초기화를 건너뜁니다.");
            return;
        }

        log.info("========== 테스트용 데이터 초기화 시작 ==========");

        // 1. 유저 4명 생성
        List<User> users = new ArrayList<>();
        users.add(createTestUser("user1", "경찰1"));
        users.add(createTestUser("user2", "경찰2"));
        users.add(createTestUser("user3", "도둑1"));
        users.add(createTestUser("user4", "도둑2"));

        // 2. 게임 생성 (방장: user1)
        Game game = createGame(users.get(0));

        // 3. 참가자 등록 및 팀 배정 (2:2)
        joinGame(game, users.get(0), Team.POLICE, true);  // 방장 (경찰)
        joinGame(game, users.get(1), Team.POLICE, false); // 경찰
        joinGame(game, users.get(2), Team.ROBBER, false); // 도둑
        joinGame(game, users.get(3), Team.ROBBER, false); // 도둑

        // 4. 게임 시작 상태로 변경
        game.startGame(LocalDateTime.now());
        gameParticipantRepository.updateStatusByGameId(game.getId(), ParticipantStatus.ALIVE);

        log.info("Game Started: [ID: {}, InviteCode: {}]", game.getId(), game.getInviteCode());
        log.info("========== 테스트용 데이터 초기화 완료 ==========");
    }

    private User createTestUser(String socialId, String nickname) {
        User user = User.signUp(socialId, SocialType.GOOGLE, nickname);
        userRepository.save(user);

        UserDevice userDevice = UserDevice.connect(
                user,
                "device-id-" + socialId,
                DeviceType.ANDROID,
                "fcm-token-" + socialId
        );
        userDeviceRepository.save(userDevice);

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        refreshTokenRepository.save(
                user.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );

        log.info("User Created: [ID: {}, Nickname: {}]", user.getId(), nickname);
        log.info("Access Token: {}", accessToken);
        
        return user;
    }

    private Game createGame(User host) {
        GameCreateCommand command = new GameCreateCommand(
                host.getId(),
                37.5665, 126.978, 1000,
                37.5665, 126.978, 100,
                30, 5, 3, 10
        );

        Game game = Game.createGame("TEST12", command);
        gameRepository.save(game);

        Point playgroundCenter = geometryFactory.createPoint(new Coordinate(command.playgroundLongitude(), command.playgroundLatitude()));
        Point jailCenter = geometryFactory.createPoint(new Coordinate(command.jailLongitude(), command.jailLatitude()));

        GameArea gameArea = GameArea.createGameArea(
                game,
                playgroundCenter,
                command.playgroundRadiusInMeters(),
                jailCenter,
                command.jailRadiusInMeters()
        );
        gameAreaRepository.save(gameArea);

        return game;
    }

    private void joinGame(Game game, User user, Team team, boolean isHost) {
        GameParticipant participant = GameParticipant.createParticipant(game, user, isHost);
        participant.changeTeam(team);
        participant.updateReady(true);
        gameParticipantRepository.save(participant);
    }
}
