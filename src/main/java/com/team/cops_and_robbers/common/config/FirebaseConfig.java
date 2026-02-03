package com.team.cops_and_robbers.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    private static final String CONFIG_KEY_PATH = "firebase-key.json";

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            ClassPathResource resource = new ClassPathResource(CONFIG_KEY_PATH);

            if (!resource.exists()) {
                throw new ApplicationException(CommonException.FIREBASE_CONFIG_NOT_FOUND);
            }

            InputStream serviceAccount = resource.getInputStream();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);

        } catch (IOException e) {
            log.error("Failed to initialize Firebase SDK. Error: {}", e.getMessage(), e);
            throw new ApplicationException(CommonException.FIREBASE_INIT_ERROR);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance(firebaseApp());
    }
}
