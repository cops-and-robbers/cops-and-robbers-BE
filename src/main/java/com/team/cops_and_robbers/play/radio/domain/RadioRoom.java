package com.team.cops_and_robbers.play.radio.domain;

public final class RadioRoom {

    private static final String CHASER_ROOM_FORMAT = "game-%d-chaser";

    private RadioRoom() {
    }

    public static String chaserRoomOf(Long gameId) {
        return String.format(CHASER_ROOM_FORMAT, gameId);
    }
}
