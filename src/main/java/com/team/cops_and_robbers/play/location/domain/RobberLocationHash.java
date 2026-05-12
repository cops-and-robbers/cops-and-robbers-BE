package com.team.cops_and_robbers.play.location.domain;

public record RobberLocationHash(
        Long participantId,
        String nickname,
        String encryptedLatitude,
        String encryptedLongitude
) {}
