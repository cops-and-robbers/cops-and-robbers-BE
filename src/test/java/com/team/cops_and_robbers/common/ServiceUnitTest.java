package com.team.cops_and_robbers.common;

import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public abstract class ServiceUnitTest {

    @Mock
    protected GameRepository gameRepository;

    @Mock
    protected GameParticipantRepository gameParticipantRepository;

    @Mock
    protected GameAreaRepository gameAreaRepository;

    @Mock
    protected UserRepository userRepository;

    @Mock
    protected UserDeviceRepository userDeviceRepository;

    @Mock
    protected JwtTokenProvider jwtTokenProvider;

    protected static void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
