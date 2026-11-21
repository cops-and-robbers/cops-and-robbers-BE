package com.team.cops_and_robbers.common.config;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQlConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.DateTime)
                // 어드민 웹 호환: GraphQL enum은 옛 이름(POLICE, ROBBER 등)을 유지한다
                .type("Team", type -> type.enumValues(Team::from))
                .type("ParticipantStatus", type -> type.enumValues(ParticipantStatus::from))
                .type("GameEndReason", type -> type.enumValues(GameEndReason::from));
    }
}
