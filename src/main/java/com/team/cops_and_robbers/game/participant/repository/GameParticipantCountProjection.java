package com.team.cops_and_robbers.game.participant.repository;

public record GameParticipantCountProjection(
        Long gameId,
        Long count
) {}
