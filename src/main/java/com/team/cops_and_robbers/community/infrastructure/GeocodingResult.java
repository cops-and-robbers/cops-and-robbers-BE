package com.team.cops_and_robbers.community.infrastructure;

import com.team.cops_and_robbers.community.domain.PostAddress;

public sealed interface GeocodingResult
        permits GeocodingResult.Resolved, GeocodingResult.NotFound, GeocodingResult.Failed {

    record Resolved(PostAddress postAddress) implements GeocodingResult {}

    record NotFound() implements GeocodingResult {}

    record Failed() implements GeocodingResult {}
}
