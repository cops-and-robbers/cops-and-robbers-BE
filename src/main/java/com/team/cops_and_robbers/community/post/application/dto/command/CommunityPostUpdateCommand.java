package com.team.cops_and_robbers.community.post.application.dto.command;

import java.time.LocalDateTime;
import java.util.function.UnaryOperator;

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

    public CommunityPostUpdateCommand withMaskedText(UnaryOperator<String> masker) {
        return new CommunityPostUpdateCommand(
                writerId, postId, masker.apply(title), masker.apply(content), meetingAt,
                latitude, longitude, masker.apply(placeName), maxParticipants);
    }
}
