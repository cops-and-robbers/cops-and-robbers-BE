package com.team.cops_and_robbers.community.application.dto.command;

import java.time.LocalDateTime;

public record CommunityPostCreateCommand(
        Long userId,
        String title,
        String content,
        LocalDateTime meetingAt,
        Double latitude,
        Double longitude,
        Integer maxParticipants
) {
}
