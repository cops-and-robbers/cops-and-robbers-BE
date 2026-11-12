package com.team.cops_and_robbers.play.radio.infrastructure;

import java.time.Duration;

public interface RadioTokenProvider {

    /** 무전 방 접속 토큰을 서명한다. identity 는 참가자 ID 다. */
    RadioToken createJoinToken(String room, Long participantId, String nickname);

    Duration tokenTtl();

    String serverUrl();
}
