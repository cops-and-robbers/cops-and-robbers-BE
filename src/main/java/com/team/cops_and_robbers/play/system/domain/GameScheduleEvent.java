package com.team.cops_and_robbers.play.system.domain;

/**
 * Redis Delayed Queue에 저장되는 게임 스케줄 이벤트.
 * roundNumber: 어느 라운드에서 발행된 이벤트인지 식별 — 조기 종료 후 재시작 시 이전 라운드 이벤트를 스킵하는 데 사용
 */
public record GameScheduleEvent(
        Long gameId,
        GameScheduleEventType type,
        int roundNumber
) {
}
