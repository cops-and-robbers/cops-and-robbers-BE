package com.team.cops_and_robbers.community.post.infrastructure;

public interface GeocodingClient {

    /** 게시글에 저장할 주소를 만든다. 표기 언어를 그 나라 언어로 맞추기 위해 벤더를 한 번 더 부를 수 있다. */
    GeocodingResult reverseGeocode(Double latitude, Double longitude);

    /**
     * 좌표가 속한 국가만 알아낸다. 목록을 국가별로 거를 때 쓴다.
     * 주소를 저장하지 않으므로 표기 언어를 맞추는 추가 호출을 생략한다.
     */
    GeocodingResult findCountry(Double latitude, Double longitude);
}
