package com.team.cops_and_robbers.loadtest;

import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.application.GameResultService;
import com.team.cops_and_robbers.play.common.application.InGameParticipantCacheService;
import com.team.cops_and_robbers.play.system.application.GameEventProducer;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 부하테스트용 유저 / 게임 시딩.
 * <p>
 * 트랜잭션 경계를 나누기 위해 러너({@link LoadTestDataLoader})와 분리했다.
 * 참가자 생성 커밋이 끝나야 {@link InGameParticipantCacheService#loadCache}가 DB에서 읽어갈 수 있으므로,
 * 방 생성과 게임 시작을 별도 트랜잭션으로 쪼갠다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoadTestSeeder {

    /** 실제 유저와 섞이지 않도록 */
    static final String SOCIAL_ID_PREFIX = "loadtest-";

    private static final int ROUND_DURATION_MINUTES = 30;
    private static final int LOCATION_REVEAL_INTERVAL_MINUTES = 5;
    private static final int POLICE_WAIT_MINUTES = 3;

    private static final double CENTER_LAT = 37.5665;
    private static final double CENTER_LNG = 126.978;
    private static final int PLAYGROUND_RADIUS_M = 1000;
    private static final int JAIL_RADIUS_M = 100;

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GameRepository gameRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final GameResultService gameResultService;
    private final GameEventProducer gameEventProducer;
    private final InGameParticipantCacheService inGameParticipantCacheService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * 경찰 / 도둑을 번갈아 배정해 1:1
     */
    @Transactional
    public RoomSeedResult seedRoom(String roomLabel, String inviteCode, int size, int userOffset) {
        GameCreateCommand command = new GameCreateCommand(
                null,
                new GameAreaData.CircleAreaData(
                        CENTER_LAT, CENTER_LNG, PLAYGROUND_RADIUS_M,
                        CENTER_LAT, CENTER_LNG, JAIL_RADIUS_M
                ),
                ROUND_DURATION_MINUTES,
                LOCATION_REVEAL_INTERVAL_MINUTES,
                POLICE_WAIT_MINUTES,
                size
        );

        Game game = Game.createGame(inviteCode, command);
        gameRepository.save(game);
        saveGameArea(game);

        List<PlayerCredential> players = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Team team = (i % 2 == 0) ? Team.POLICE : Team.ROBBER;
            String socialId = SOCIAL_ID_PREFIX + (userOffset + i);
            String nickname = roomLabel + "-" + (team == Team.POLICE ? "P" : "R") + (i / 2 + 1);

            User user = createUser(socialId, nickname);
            String accessToken = issueTokens(user);
            joinGame(game, user, team, i == 0);

            players.add(new PlayerCredential(user.getId(), accessToken, game.getId(), team.name(), roomLabel));
        }

        log.info("[LoadTest] Room {} seeded: gameId={}, inviteCode={}, size={}", roomLabel, game.getId(), inviteCode, size);
        return new RoomSeedResult(game.getId(), players);
    }

    /**
     * 게임을 진행 상태로 만든다.
     */
    @Transactional
    public void startGame(Long gameId) {
        Game game = gameRepository.getByGameId(gameId);
        game.startGame(LocalDateTime.now());
        gameParticipantRepository.updateStatusByGameId(gameId, ParticipantStatus.ALIVE);
        gameResultService.openGameResult(game);
    }

    /** 트랜잭션 커밋 이후에 호출해야 캐시가 참가자를 모두 읽어간다. */
    public void scheduleAndLoadCache(Long gameId) {
        gameEventProducer.scheduleAllEvents(gameId);
        inGameParticipantCacheService.loadCache(gameId);
        log.info("[LoadTest] Game {} started: schedule + cache loaded", gameId);
    }

    private void saveGameArea(Game game) {
        Point playgroundCenter = geometryFactory.createPoint(new Coordinate(CENTER_LNG, CENTER_LAT));
        Point jailCenter = geometryFactory.createPoint(new Coordinate(CENTER_LNG, CENTER_LAT));
        gameAreaRepository.save(GameArea.createCircleGameArea(
                game, playgroundCenter, PLAYGROUND_RADIUS_M, jailCenter, JAIL_RADIUS_M
        ));
    }

    private User createUser(String socialId, String nickname) {
        User user = User.signUp(socialId, SocialType.GOOGLE, nickname);
        userRepository.save(user);

        userDeviceRepository.save(UserDevice.connect(
                user, "device-" + socialId, DeviceType.ANDROID, "fcm-" + socialId
        ));
        return user;
    }

    private String issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        refreshTokenRepository.save(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpirationMillis());
        return accessToken;
    }

    private void joinGame(Game game, User user, Team team, boolean isHost) {
        GameParticipant participant = GameParticipant.createParticipant(game, user, isHost);
        participant.changeTeam(team);
        participant.updateReady(true);
        gameParticipantRepository.save(participant);
    }

    public record RoomSeedResult(Long gameId, List<PlayerCredential> players) {
    }

    public record PlayerCredential(Long userId, String token, Long gameId, String team, String room) {
    }
}
