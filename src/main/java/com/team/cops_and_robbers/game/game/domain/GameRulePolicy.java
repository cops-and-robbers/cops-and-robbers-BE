package com.team.cops_and_robbers.game.game.domain;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.area.exception.GameAreaException;
import com.team.cops_and_robbers.game.game.exception.GameException;
import org.springframework.stereotype.Service;

/**
 * 게임 규칙에 따라 허용되지 않는 요청을 거절합니다.
 */
@Service
public class GameRulePolicy {

    public void validateForceArrest(Game game) {
        if (!game.rules().allowsForceArrest()) {
            throw new ApplicationException(GameException.FORCE_ARREST_NOT_ALLOWED_IN_EVENT_GAME);
        }
    }

    public void validateArea(Game game, GameAreaData areaData) {
        boolean usesJailArea = game.rules().usesJailArea();
        if (usesJailArea && !areaData.hasJail()) {
            throw new ApplicationException(GameAreaException.JAIL_AREA_REQUIRED);
        }
        if (!usesJailArea && areaData.hasJail()) {
            throw new ApplicationException(GameAreaException.JAIL_AREA_NOT_ALLOWED);
        }
    }
}
