    package com.team.cops_and_robbers.game.participant.domain;

    import java.util.Arrays;
    import java.util.Collections;
    import java.util.List;

    public enum Team {
        POLICE,
        ROBBER;

        private static final List<Team> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = VALUES.size();
        private static final java.util.Random RANDOM = new java.util.Random();

        public static Team getRandomTeam() {
            return VALUES.get(RANDOM.nextInt(SIZE));
        }
    }
