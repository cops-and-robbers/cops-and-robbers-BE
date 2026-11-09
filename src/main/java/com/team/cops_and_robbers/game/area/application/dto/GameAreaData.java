package com.team.cops_and_robbers.game.area.application.dto;

import com.team.cops_and_robbers.common.dto.Coordinates;

import java.util.List;

public sealed interface GameAreaData permits GameAreaData.CircleAreaData, GameAreaData.PolygonAreaData {

    boolean hasJail();

    record CircleAreaData(
            Double playgroundLatitude,
            Double playgroundLongitude,
            Integer playgroundRadiusInMeters,
            Double jailLatitude,
            Double jailLongitude,
            Integer jailRadiusInMeters
    ) implements GameAreaData {

        @Override
        public boolean hasJail() {
            return jailLatitude != null && jailLongitude != null && jailRadiusInMeters != null;
        }
    }
    record PolygonAreaData(
            List<Coordinates> playgroundPolygon,
            List<Coordinates> jailPolygon
    ) implements GameAreaData {

        @Override
        public boolean hasJail() {
            return jailPolygon != null;
        }
    }
}
