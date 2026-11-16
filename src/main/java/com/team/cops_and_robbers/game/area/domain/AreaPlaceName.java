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

    @Column(name = "coordinate_key", nullable = false, unique = true, length = 20)
    private String coordinateKey;

    @Column(name = "place_name", nullable = false)
    private String placeName;

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
