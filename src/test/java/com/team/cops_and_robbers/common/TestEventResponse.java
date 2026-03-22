package com.team.cops_and_robbers.common;

import com.fasterxml.jackson.databind.JsonNode;

public record TestEventResponse(
        String eventId,
        Long gameId,
        String type,
        String timestamp,
        JsonNode data
) {
}
