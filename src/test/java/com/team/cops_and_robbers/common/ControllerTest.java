package com.team.cops_and_robbers.common;

import com.google.firebase.auth.FirebaseAuth;
import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.infrastructure.social.strategy.AppleLoginStrategy;
import com.team.cops_and_robbers.auth.infrastructure.social.strategy.GoogleLoginStrategy;
import com.team.cops_and_robbers.auth.infrastructure.social.strategy.KakaoLoginStrategy;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.bug.repository.BugReportRepository;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.common.fixture.UserDeviceFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.community.infrastructure.GeocodingClient;
import com.team.cops_and_robbers.community.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.repository.GameResultRepository;
import com.team.cops_and_robbers.notice.repository.NoticeRepository;
import com.team.cops_and_robbers.play.notification.application.GameFcmNotifier;
import com.team.cops_and_robbers.report.repository.ReportRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@Sql(scripts = "/truncate.sql")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ControllerTest {

    @MockitoBean
    protected FirebaseAuth firebaseAuth;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    protected KakaoLoginStrategy kakaoLoginStrategy;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    protected GoogleLoginStrategy googleLoginStrategy;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    protected AppleLoginStrategy appleLoginStrategy;

    @MockitoBean
    protected FcmService fcmService;

    @MockitoBean
    protected GameFcmNotifier gameFcmNotifier;

    @MockitoBean
    protected GeocodingClient geocodingClient;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected GameRepository gameRepository;

    @Autowired
    protected GameParticipantRepository gameParticipantRepository;

    @Autowired
    protected GameAreaRepository gameAreaRepository;

    @Autowired
    protected GameResultRepository gameResultRepository;

    @Autowired
    protected UserDeviceRepository userDeviceRepository;

    @Autowired
    protected BugReportRepository bugReportRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected NoticeRepository noticeRepository;

    @Autowired
    protected ReportRepository reportRepository;

    @Autowired
    protected CommunityPostRepository communityPostRepository;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        doReturn(GeocodingResult.failed())
                .when(geocodingClient)
                .reverseGeocode(any(), any());
    }

    protected String givenAccessToken(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }

    protected User givenUser() {
        return userRepository.save(UserFixture.USER());
    }

    protected User givenUser(String nickname) {
        return userRepository.save(UserFixture.USER(nickname));
    }

    protected User givenAdminUser() {
        return userRepository.save(UserFixture.ADMIN());
    }

    protected User givenAdminUser(String nickname) {
        return userRepository.save(UserFixture.ADMIN(nickname));
    }

    protected UserDevice givenUserDevice() {
        return userDeviceRepository.save(UserDeviceFixture.IOS_DEVICE(givenUser()));
    }

    protected Tokens givenTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        refreshTokenRepository.save(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpirationMillis());
        return new Tokens(accessToken, refreshToken);
    }

    protected GameParticipant givenHost(Game game, User user) {
        return gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, user));
    }

    protected GameParticipant givenGuest(Game game, User user) {
        return gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(game, user));
    }

    protected GameParticipant givenPolice(Game game, User user) {
        return gameParticipantRepository.save(GameParticipantFixture.ALIVE_POLICE(game, user));
    }

    protected GameParticipant givenRobber(Game game, User user) {
        return gameParticipantRepository.save(GameParticipantFixture.ALIVE_ROBBER(game, user));
    }

    protected GameParticipant givenJailedRobber(Game game, User user) {
        return gameParticipantRepository.save(GameParticipantFixture.JAILED_ROBBER(game, user));
    }

    protected GameParticipant givenInGameHost(Game game, User user) {
        return gameParticipantRepository.save(GameParticipantFixture.HOST_WAITING_POLICE(game, user));
    }

    protected GameParticipant givenWaitingPolice(Game game, User user) {
        GameParticipant participant = gameParticipantRepository.getByGameIdAndUserId(game.getId(), user.getId());
        participant.updateStatus(ParticipantStatus.POLICE_WAITING);
        return gameParticipantRepository.save(participant);
    }

    protected RequestSpecification authenticated(String token) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token);
    }

    protected RequestSpecification unauthenticated() {
        return given()
                .contentType(ContentType.JSON);
    }
}
