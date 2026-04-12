package com.team.cops_and_robbers.game.area.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.exception.GameAreaException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameAreaRepository extends JpaRepository <GameArea, Long>{

    Optional<GameArea> findByGameId(Long gameId);

    @Modifying(clearAutomatically = true)
    @Query("delete from GameArea ga where ga.game.id = :gameId")
    void deleteByGameId(Long gameId);

    default GameArea getByGameId(Long gameId) {
        return findByGameId(gameId)
                .orElseThrow(() -> new ApplicationException(GameAreaException.GAME_AREA_NOT_FOUND));
    }

    /**
     * postgis를 활용하여 내부 원이 외부 원에 완전히 포함되는지 검증합니다.
     *
     * @return 포함되면 true, 아니면 false
     */
    @Query(value = """
    select 
        st_distance(
            st_makepoint(:outerlon, :outerlat)::geography,
            st_makepoint(:innerlon, :innerlat)::geography
        ) + :innerradius <= :outerradius
    """, nativeQuery = true)
    boolean isCircleContained(
            @Param("outerlon") double outerlon,
            @Param("outerlat") double outerlat,
            @Param("outerradius") int outerradius,
            @Param("innerlon") double innerlon,
            @Param("innerlat") double innerlat,
            @Param("innerradius") int innerradius
    );
}
