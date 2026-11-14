package com.team.cops_and_robbers.game.area.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좌표에 대응하는 동네 이름(시도, 시군구, 읍면동)과 국가입니다.
 * 어드민에서 게임 구역을 볼 때마다 주소 조회를 부르지 않도록 한 번 찾은 이름을 남겨 둡니다.
 * 저장은 {@link com.team.cops_and_robbers.game.area.repository.AreaPlaceNameRepository#insertIfAbsent} 로 합니다.
 */
@Entity
@Table(name = "area_place_names")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AreaPlaceName extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소수 셋째 자리까지 반올림한 {@code 위도,경도} 입니다. 가까운 곳은 같은 키로 묶입니다. */
    @Column(name = "coordinate_key", nullable = false, unique = true, length = 20)
    private String coordinateKey;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    /**
     * ISO 3166-1 alpha-2 국가 코드(대문자)입니다. 주소 조회가 국가를 주지 않은 곳은 null 입니다.
     * 유저별로 주로 플레이하는 국가를 셀 때 씁니다.
     */
    @Column(name = "country_code", length = 2)
    private String countryCode;

    public static AreaPlaceName create(String coordinateKey, String placeName, String countryCode) {
        return AreaPlaceName.builder()
                .coordinateKey(coordinateKey)
                .placeName(placeName)
                .countryCode(countryCode)
                .build();
    }
}
