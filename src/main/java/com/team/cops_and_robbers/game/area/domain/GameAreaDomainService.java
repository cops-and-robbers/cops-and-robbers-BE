package com.team.cops_and_robbers.game.area.domain;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.exception.GameAreaException;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameAreaDomainService {

    private final GameAreaRepository gameAreaRepository;

    /**
     * 게임 구역의 공간적 포함 관계를 검증합니다.
     *
     * 1. 감옥 반지름 < 플레이그라운드 반지름
     * 2. 감옥이 플레이그라운드 영역 내에 완전히 포함
     * 구현: postgis의 st_distance를 활용
     *
     */
    public void validateAreaContainment(
            double playgroundLon, double playgroundLat, int playgroundRadius,
            double jailLon, double jailLat, int jailRadius) {

        if (jailRadius >= playgroundRadius) {
            throw new ApplicationException(GameAreaException.INVALID_JAIL_RADIUS);
        }

        boolean isContained = gameAreaRepository.isCircleContained(
                playgroundLon, playgroundLat, playgroundRadius,
                jailLon, jailLat, jailRadius
        );

        if (!isContained) {
            throw new ApplicationException(GameAreaException.JAIL_OUTSIDE_PLAYGROUND);
        }
    }
}
