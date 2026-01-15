package com.team.cops_and_robbers.game.area.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.exception.GameAreaException;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GameAreaRepository extends JpaRepository <GameArea, Long>{

    Optional<GameArea> findByGameId(Long gameId);

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
            double outerlon,
            double outerlat,
            int outerradius,
            double innerlon,
            double innerlat,
            int innerradius
    );
}
