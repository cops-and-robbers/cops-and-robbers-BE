package com.team.cops_and_robbers.play.radio.domain;

public final class RadioRoom {

    private static final String POLICE_ROOM_FORMAT = "game-%d-police";

    private RadioRoom() {
    }

    public static String policeRoomOf(Long gameId) {
        return String.format(POLICE_ROOM_FORMAT, gameId);
    }
}
