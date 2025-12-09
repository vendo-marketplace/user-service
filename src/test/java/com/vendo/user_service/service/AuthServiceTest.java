package com.vendo.user_service.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.security.common.exception.AccessDeniedException;
import com.vendo.user_service.common.builder.TokenPayloadDataBuilder;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.model.User;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.service.user.UserService;
import com.vendo.user_service.service.auth.AuthService;
import com.vendo.user_service.service.auth.GoogleOAuthService;
import com.vendo.user_service.web.dto.AuthResponse;
import com.vendo.user_service.web.dto.GoogleAuthRequest;
import com.vendo.user_service.web.dto.UserUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleOAuthService googleOauthService;

    @Test
    void googleAuth_shouldReturnTokenPayload() {
        TokenPayload tokenPayload = TokenPayloadDataBuilder.buildTokenPayloadWithAllFields().build();
        GoogleAuthRequest googleAuthRequest = new GoogleAuthRequest("test_id_token");
        User user = UserDataBuilder.buildUserAllFields().build();
        String idToken = "test_id_token";
        String email = "test_email";
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);

        when(googleOauthService.verify(idToken)).thenReturn(mockPayload);
        when(mockPayload.getEmail()).thenReturn(email);
        when(userService.findUserByEmailOrSave(email)).thenReturn(user);
        when(jwtService.generateTokenPayload(user)).thenReturn(tokenPayload);

        verify(userService, never()).save(user);
        AuthResponse authResponse = authService.googleAuth(googleAuthRequest);
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isEqualTo(tokenPayload.accessToken());
        assertThat(authResponse.refreshToken()).isEqualTo(tokenPayload.refreshToken());

        ArgumentCaptor<UserUpdateRequest> userArgumentCaptor = ArgumentCaptor.forClass(UserUpdateRequest.class);
        verify(googleOauthService).verify(idToken);
        verify(userService).findUserByEmailOrSave(email);
        verify(jwtService).generateTokenPayload(user);
        verify(userService).update(eq(user.getId()), userArgumentCaptor.capture());

        UserUpdateRequest userCaptorValue = userArgumentCaptor.getValue();
        assertThat(userCaptorValue).isNotNull();
        assertThat(userCaptorValue.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userCaptorValue.providerType()).isEqualTo(ProviderType.GOOGLE);
    }

    @Test
    void googleAuth_shouldActivateIncompletedUser_andReturnTokenPayload() {
        TokenPayload tokenPayload = TokenPayloadDataBuilder.buildTokenPayloadWithAllFields().build();
        GoogleAuthRequest googleAuthRequest = new GoogleAuthRequest("test_id_token");
        User user = UserDataBuilder.buildUserAllFields().build();
        String idToken = "test_id_token";
        String email = "test_email";
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);

        when(googleOauthService.verify(idToken)).thenReturn(mockPayload);
        when(mockPayload.getEmail()).thenReturn(email);
        when(userService.findUserByEmailOrSave(email)).thenReturn(user);
        when(jwtService.generateTokenPayload(user)).thenReturn(tokenPayload);

        AuthResponse authResponse = authService.googleAuth(googleAuthRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isEqualTo(tokenPayload.accessToken());
        assertThat(authResponse.refreshToken()).isEqualTo(tokenPayload.refreshToken());

        ArgumentCaptor<UserUpdateRequest> userCaptor = ArgumentCaptor.forClass(UserUpdateRequest.class);
        verify(googleOauthService).verify(idToken);
        verify(userService).findUserByEmailOrSave(email);
        verify(jwtService).generateTokenPayload(user);
        verify(userService).update(eq(user.getId()), userCaptor.capture());

        UserUpdateRequest captorValue = userCaptor.getValue();
        assertThat(captorValue.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(captorValue.providerType()).isEqualTo(ProviderType.GOOGLE);
    }

    @Test
    void googleAuth_shouldNotUpdateProviderTypeToGoogle_whenUserIsActive() {
        TokenPayload tokenPayload = TokenPayloadDataBuilder.buildTokenPayloadWithAllFields().build();
        GoogleAuthRequest googleAuthRequest = new GoogleAuthRequest("test_id_token");
        User user = UserDataBuilder.buildUserAllFields().status(UserStatus.ACTIVE).build();
        String idToken = "test_id_token";
        String email = "test_email";
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);

        when(googleOauthService.verify(idToken)).thenReturn(mockPayload);
        when(mockPayload.getEmail()).thenReturn(email);
        when(userService.findUserByEmailOrSave(email)).thenReturn(user);
        when(jwtService.generateTokenPayload(user)).thenReturn(tokenPayload);

        AuthResponse authResponse = authService.googleAuth(googleAuthRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isEqualTo(tokenPayload.accessToken());
        assertThat(authResponse.refreshToken()).isEqualTo(tokenPayload.refreshToken());

        verify(googleOauthService).verify(idToken);
        verify(userService).findUserByEmailOrSave(email);
        verify(jwtService).generateTokenPayload(user);
        verify(userService, never()).update(eq(user.getId()), any(UserUpdateRequest.class));
    }

    @Test
    void googleAuth_shouldThrowException_whenIdTokenNotVerified() {
        GoogleAuthRequest googleAuthRequest = new GoogleAuthRequest("test_id_token");
        User user = UserDataBuilder.buildUserAllFields().build();
        String idToken = "test_id_token";
        String email = "test_email";

        when(googleOauthService.verify(idToken)).thenThrow(AccessDeniedException.class);

        assertThatThrownBy(() -> authService.googleAuth(googleAuthRequest))
                .isInstanceOf(AccessDeniedException.class);

        verify(googleOauthService).verify(idToken);
        verify(userService, never()).findUserByEmailOrSave(email);
        verify(jwtService, never()).generateTokenPayload(user);
    }
}
