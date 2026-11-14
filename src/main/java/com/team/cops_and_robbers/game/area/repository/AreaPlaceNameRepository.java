package com.team.cops_and_robbers.game.area.repository;

import com.team.cops_and_robbers.game.area.domain.AreaPlaceName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AreaPlaceNameRepository extends JpaRepository<AreaPlaceName, Long> {

    List<AreaPlaceName> findByCoordinateKeyIn(Collection<String> coordinateKeys);

    /**
     * 같은 곳을 동시에 처음 보면 같은 키가 두 번 들어올 수 있어, 이미 있으면 먼저 들어간 이름을 남깁니다.
     * save 로 넣으면 중복 키 예외가 에러 로그로 남아 실제 장애와 구분되지 않습니다.
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO area_place_names (coordinate_key, place_name, country_code, created_at, updated_at)
            VALUES (:coordinateKey, :placeName, :countryCode, :now, :now)
            ON CONFLICT (coordinate_key) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("coordinateKey") String coordinateKey,
            @Param("placeName") String placeName,
            @Param("countryCode") String countryCode,
            @Param("now") LocalDateTime now
    );
}
