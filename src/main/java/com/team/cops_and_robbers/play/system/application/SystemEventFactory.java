package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SystemEventFactory {

    public SystemEvent createPoliceMoveStartEvent(Long gameId) {
        SystemEventData.PoliceMoveStartData data = SystemEventData.PoliceMoveStartData.create();
        return SystemEvent.of(gameId, SystemEventType.POLICE_MOVE_START, data);
    }

    public SystemEvent createRobberLocationRevealEvent(Long gameId, List<SystemEventData.RobberLocation> locations) {
        SystemEventData.RobberLocationRevealData data = SystemEventData.RobberLocationRevealData.of(locations);
        return SystemEvent.of(gameId, SystemEventType.ROBBER_LOCATION_REVEAL, data);
    }
}
