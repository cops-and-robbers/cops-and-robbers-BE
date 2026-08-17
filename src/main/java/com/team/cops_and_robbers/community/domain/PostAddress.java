package com.team.cops_and_robbers.community.domain;

import org.springframework.util.StringUtils;

public record PostAddress(
        String address,
        String roadAddress,
        String buildingName,
        String region
) {
    private static final PostAddress EMPTY = new PostAddress(null, null, null, null);

    public PostAddress {
        address = textOrNull(address);
        roadAddress = textOrNull(roadAddress);
        buildingName = textOrNull(buildingName);
        region = textOrNull(region);
    }

    public static PostAddress of(String address, String roadAddress, String buildingName, String region) {
        return new PostAddress(address, roadAddress, buildingName, region);
    }

    public static PostAddress empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return address == null && roadAddress == null && buildingName == null && region == null;
    }

    private static String textOrNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
