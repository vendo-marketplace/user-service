package com.vendo.user_service.service.user.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.vendo.security.common.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOauthService {

    @Value("${google.client_id}")
    private String GOOGLE_CLIENT_ID;

    public GoogleIdToken.Payload verify(String token) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                .build();

        try {
            GoogleIdToken googleIdToken = verifier.verify(token);
            return googleIdToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            log.error(e.getMessage());
            throw new AccessDeniedException("Invalid ID token.");
        }
    }
}
