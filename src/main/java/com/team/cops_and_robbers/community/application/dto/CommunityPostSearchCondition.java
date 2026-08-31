package com.team.cops_and_robbers.community.application.dto;

import com.team.cops_and_robbers.community.domain.CommunityPostSort;

import java.util.List;

/** countryCode와 excludeCountryCodes 중 정확히 하나만 채워진다. */
public record CommunityPostSearchCondition(
        String countryCode,
        List<String> excludeCountryCodes,
        CommunityPostSort sort,
        Double latitude,
        Double longitude,
        String keyword
) {
}
