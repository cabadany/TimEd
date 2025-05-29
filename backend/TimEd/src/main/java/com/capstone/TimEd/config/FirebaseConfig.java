package com.capstone.TimEd.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

@Configuration
public class FirebaseConfig {

    private final Environment env;

    public FirebaseConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount;
            
            // Check if FIREBASE_SERVICE_ACCOUNT_KEY environment variable exists (for production)
            String serviceAccountJson = env.getProperty("FIREBASE_SERVICE_ACCOUNT_KEY");
            
            if (serviceAccountJson != null && !serviceAccountJson.isEmpty()) {
                // Production mode: use environment variable
                System.out.println("Using Firebase service account from environment variable");
                serviceAccount = new ByteArrayInputStream(serviceAccountJson.getBytes());
            } else {
                // Development mode: use local file
                System.out.println("Using Firebase service account from local file");
                serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();
            }
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId("timed-system")  // Ensure your project ID is set
                    .setDatabaseUrl("https://timed-system-default-rtdb.firebaseio.com/")  // Add your Realtime Database URL here
                    .build();
            
            return FirebaseApp.initializeApp(options);
        } else {
            return FirebaseApp.getInstance();
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        return FirebaseAuth.getInstance(firebaseApp());
    }

    @Bean
    public Firestore firestore() throws IOException {
        return FirestoreClient.getFirestore(firebaseApp());
    }
}
