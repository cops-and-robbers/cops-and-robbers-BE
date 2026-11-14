package com.team.cops_and_robbers.admin.application.dto.result.user;

/**
 * 유저가 끝까지 마친 게임을 플레이 구역의 국가로 묶은 한 줄입니다.
 *
 * @param countryCode  ISO 3166-1 alpha-2 국가 코드(대문자). 아직 국가를 찾지 못한 판을 묶은 줄이면 null
 * @param games        그 국가에서 한 판 수
 * @param lastPlayedAt 그 국가에서 마지막으로 끝난 판의 시각(KST ISO)
 */
public record AdminPlayCountryResult(String countryCode, int games, String lastPlayedAt) {
}
