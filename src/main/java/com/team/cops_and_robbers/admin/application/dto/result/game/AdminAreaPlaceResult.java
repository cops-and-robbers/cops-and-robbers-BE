package com.team.cops_and_robbers.admin.application.dto.result.game;

/**
 * 플레이 구역의 동네 이름과 국가입니다.
 *
 * @param placeName   시도·시군구·읍면동 (예: 서울특별시 광진구 화양동)
 * @param countryCode ISO 3166-1 alpha-2 국가 코드(대문자). 주소 조회가 국가를 주지 않은 곳은 null
 */
public record AdminAreaPlaceResult(String placeName, String countryCode) {
}
