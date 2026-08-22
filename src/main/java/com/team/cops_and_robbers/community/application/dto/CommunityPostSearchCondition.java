package com.team.cops_and_robbers.community.application.dto;

import com.team.cops_and_robbers.community.domain.CommunityPostSort;

public record CommunityPostSearchCondition(
        String countryCode,
        CommunityPostSort sort,
        Double latitude,
        Double longitude,
        String keyword
) {
}
