package com.vendo.user_service.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.vendo.user_service.service.user.auth.GoogleOauthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class GoogleOauthServiceTest {

    @InjectMocks
    private GoogleOauthService googleOauthService;

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Test
    void shouldVerifyToken_andReturnPayload() throws GeneralSecurityException, IOException {
        GoogleIdToken mockedIdToken = mock(GoogleIdToken.class);
        String idToken = "test_id_token";

        when(googleIdTokenVerifier.verify(idToken)).thenReturn(mockedIdToken);

        GoogleIdToken.Payload verify = googleOauthService.verify(idToken);
        System.out.println(verify);
    }

}
