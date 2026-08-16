package com.team.cops_and_robbers.community.domain;

import org.springframework.util.StringUtils;

public record PostAddress(
        String address,
        String roadAddress,
        String buildingName
) {
    private static final PostAddress EMPTY = new PostAddress(null, null, null);

    public PostAddress {
        address = textOrNull(address);
        roadAddress = textOrNull(roadAddress);
        buildingName = textOrNull(buildingName);
    }

    public static PostAddress empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return address == null && roadAddress == null && buildingName == null;
    }

    private static String textOrNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
