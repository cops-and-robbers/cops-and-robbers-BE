package com.team.cops_and_robbers.play.lobby.application.dto.command;

public record KickCommand (
    Long userId,
    Long gameId,
    Long targetParticipantId
){
    public static KickCommand of(Long userId, Long gameId, Long kickedParticipantId) {
        return new KickCommand(userId, gameId, kickedParticipantId);
    }
}

