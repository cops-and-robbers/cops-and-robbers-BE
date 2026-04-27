package com.team.cops_and_robbers.game.participant.repository;

import com.team.cops_and_robbers.game.participant.domain.Team;

public record GameParticipantCacheProjection(
        Long participantId,
        String nickname,
        Team team,
        String fcmToken
) {}
