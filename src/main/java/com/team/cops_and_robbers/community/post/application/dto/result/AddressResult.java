package com.team.cops_and_robbers.community.post.application.dto.result;

import com.team.cops_and_robbers.community.post.domain.PostAddress;

public record AddressResult(
        String region,
        String address,
        String countryCode
) {
    public static AddressResult from(PostAddress postAddress) {
        return new AddressResult(postAddress.region(), postAddress.address(), postAddress.countryCode());
    }
}
