package com.vendo.user_service.service.user.auth.common.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class GoogleIdTokenVerifierConfig {

    @Value("${google.client_id}")
    private String GOOGLE_CLIENT_ID;

    private final NetHttpTransport netHttpTransport = new NetHttpTransport();

    private final GsonFactory gsonFactory = new GsonFactory();

    @Bean
    public GoogleIdTokenVerifier getGoogleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(netHttpTransport, gsonFactory)
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                .build();
    }
}
