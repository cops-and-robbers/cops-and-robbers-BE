package com.team.cops_and_robbers.play.location.domain;

public record RunnerLocationHash(
        Long participantId,
        String nickname,
        String encryptedLatitude,
        String encryptedLongitude
) {}
