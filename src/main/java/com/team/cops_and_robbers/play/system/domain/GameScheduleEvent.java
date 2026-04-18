package com.team.cops_and_robbers.play.system.domain;

/**
 * Redis Delayed Queue에 저장되는 게임 스케줄 이벤트.
 */
public record GameScheduleEvent(
        Long gameId,
        GameScheduleEventType type
) {
}
