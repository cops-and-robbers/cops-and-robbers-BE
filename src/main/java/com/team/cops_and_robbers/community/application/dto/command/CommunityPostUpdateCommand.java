package com.team.cops_and_robbers.community.application.dto.command;

import java.time.LocalDateTime;

public record CommunityPostUpdateCommand(
        Long writerId,
        Long postId,
        String title,
        String content,
        LocalDateTime meetingAt,
        Double latitude,
        Double longitude,
        String placeName,
        Integer maxParticipants
) {
}
