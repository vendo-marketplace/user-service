package com.vendo.user_service.service.user.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.vendo.security.common.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOauthService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public GoogleIdToken.Payload verify(String token) {
        try {
            GoogleIdToken googleIdToken = googleIdTokenVerifier.verify(token);

            if (googleIdToken == null) {
                throw new AccessDeniedException("Id token is not verified.");
            }

            return googleIdToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Google Id token security check failed: {}", e.getMessage());
            throw new AccessDeniedException("Invalid Id token.");
        }
    }
}
