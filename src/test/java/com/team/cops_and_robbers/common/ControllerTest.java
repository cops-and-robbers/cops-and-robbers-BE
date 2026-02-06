package com.team.cops_and_robbers.common;

import com.google.firebase.auth.FirebaseAuth;
import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.infrastructure.social.strategy.AppleLoginStrategy;
import com.team.cops_and_robbers.auth.infrastructure.social.strategy.GoogleLoginStrategy;
import com.team.cops_and_robbers.auth.infrastructure.social.strategy.KakaoLoginStrategy;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.common.fixture.UserDeviceFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@Sql("/truncate.sql")
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

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected GameRepository gameRepository;

    @Autowired
    protected GameParticipantRepository gameParticipantRepository;

    @Autowired
    protected GameAreaRepository gameAreaRepository;

    @Autowired
    protected UserDeviceRepository userDeviceRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    protected String givenAccessToken(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }

    protected User givenUser() {
        return userRepository.save(UserFixture.USER());
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

    protected ExtractableResponse<Response> postWithoutAuthorization(String url, Object body) {
        return RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(url)
                .then().log().all()
                .extract();
    }
}
