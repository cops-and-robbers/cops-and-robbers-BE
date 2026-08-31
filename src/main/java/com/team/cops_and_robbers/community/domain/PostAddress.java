package com.team.cops_and_robbers.community.domain;

import org.springframework.util.StringUtils;

import java.util.Locale;

public record PostAddress(
        String address,
        String roadAddress,
        String buildingName,
        String region,
        String countryCode
) {
    private static final PostAddress EMPTY = new PostAddress(null, null, null, null, null);

    public PostAddress {
        address = textOrNull(address);
        roadAddress = textOrNull(roadAddress);
        buildingName = textOrNull(buildingName);
        region = textOrNull(region);
        countryCode = toUpperCaseOrNull(countryCode);
    }

    public static PostAddress of(String address, String roadAddress, String buildingName,
                                 String region, String countryCode) {
        return new PostAddress(address, roadAddress, buildingName, region, countryCode);
    }

    public static PostAddress empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return address == null && roadAddress == null && buildingName == null
                && region == null && countryCode == null;
    }

    private static String textOrNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    /** 벤더가 소문자로 주기도 해서(Geoapify는 jp) ISO 3166-1 alpha-2 관례대로 대문자로 맞춘다. */
    private static String toUpperCaseOrNull(String countryCode) {
        return StringUtils.hasText(countryCode) ? countryCode.toUpperCase(Locale.ROOT) : null;
    }
}