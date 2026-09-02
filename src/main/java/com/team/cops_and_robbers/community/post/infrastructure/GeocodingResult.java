package com.team.cops_and_robbers.community.post.infrastructure;

import com.team.cops_and_robbers.community.post.domain.PostAddress;

public sealed interface GeocodingResult
        permits GeocodingResult.Resolved, GeocodingResult.NotFound, GeocodingResult.Failed {

    static GeocodingResult resolved(PostAddress postAddress) {
        return new Resolved(postAddress);
    }

    /** 정상 응답이지만 해당 좌표에 주소가 없는 경우. 국내 조회가 이 값을 주면 해외로 넘긴다. */
    static GeocodingResult notFound() {
        return new NotFound();
    }

    /** 호출 자체가 실패한 경우. 주소 없음과 구분해야 장애 시 대응 경로를 나눌 수 있다. */
    static GeocodingResult failed() {
        return new Failed();
    }

    record Resolved(PostAddress postAddress) implements GeocodingResult {}

    record NotFound() implements GeocodingResult {}

    record Failed() implements GeocodingResult {}
}
