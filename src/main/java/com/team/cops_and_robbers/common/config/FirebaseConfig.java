package com.team.cops_and_robbers.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    private static final String LOCAL_CLASSPATH_PREFIX = "classpath:";

    @Value("${firebase.key-path}")
    private String firebaseKeyPath;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        try (InputStream serviceAccount = getServiceAccountStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);

        } catch (IOException e) {
            log.error("[Firebase config] Failed to initialize Firebase. Path: {}", firebaseKeyPath, e);
            throw new ApplicationException(CommonException.FIREBASE_INIT_ERROR);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private InputStream getServiceAccountStream() throws IOException {
        // 로컬 개발 환경 (classpath:)
        if (firebaseKeyPath.startsWith(LOCAL_CLASSPATH_PREFIX)) {
            String path = firebaseKeyPath.substring(LOCAL_CLASSPATH_PREFIX.length());
            ClassPathResource resource = new ClassPathResource(path);

            if (!resource.exists()) {
                throw new ApplicationException(CommonException.FIREBASE_CONFIG_NOT_FOUND);
            }
            log.info("[Firebase config] Loaded key from ClassPath (Local): {}", path);
            return resource.getInputStream();
        }
        // 배포 환경
        else {
            log.info("[Firebase config] Loaded key from File System (Prod): {}", firebaseKeyPath);
            return new FileInputStream(firebaseKeyPath);
        }
    }
}
