package com.team.cops_and_robbers.auth.social.strategy;

import com.team.cops_and_robbers.user.domain.SocialType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class SocialLoginStrategyConfig {

    @Bean
    public Map<SocialType, SocialLoginStrategy> socialLoginStrategyMap(
            List<SocialLoginStrategy> strategies
    ) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        SocialLoginStrategy::getSocialType,
                        Function.identity()
                ));
    }
}
