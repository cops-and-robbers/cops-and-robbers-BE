package com.team.cops_and_robbers.common.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class FirebaseTestConfig {

    @Bean
    @Primary
    public FirebaseAuth firebaseAuth() {
        return mock(FirebaseAuth.class);
    }

    @Bean
    @Primary
    public FirebaseMessaging firebaseMessaging() {
        return mock(FirebaseMessaging.class);
    }
}
