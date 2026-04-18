package com.team.cops_and_robbers.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisQueue {

    GAME_SCHEDULE("game:schedule:events");

    private final String name;
}
