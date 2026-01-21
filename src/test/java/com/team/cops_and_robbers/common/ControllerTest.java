package com.team.cops_and_robbers.common;

import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import io.restassured.RestAssured;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@Sql("/truncate.sql")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ControllerTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected GameRepository gameRepository;

    @Autowired
    protected GameParticipantRepository participantRepository;

    @Autowired
    protected GameAreaRepository areaRepository;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
}