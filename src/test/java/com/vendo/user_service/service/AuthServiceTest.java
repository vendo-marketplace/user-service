package com.vendo.user_service.service;

import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.builder.AuthRequestDataBuilder;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.model.User;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.security.service.JwtUserDetailsService;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.RefreshRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUserDetailsService jwtUserDetailsService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Test
    public void signUp_shouldSuccessfullyRegisterUser() {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();
        String encodedPassword = "encodedPassword";

        doNothing().when(userService).throwIfUserExistsByEmail(authRequest.email());
        when(passwordEncoder.encode(authRequest.password())).thenReturn(encodedPassword);

        authService.signUp(authRequest);

        verify(userService).throwIfUserExistsByEmail(authRequest.email());
        verify(passwordEncoder).encode(authRequest.password());
        verify(userService).save(argThat(user ->
                        user.getEmail().equals(authRequest.email()) &&
                        user.getPassword().equals(encodedPassword))
        );
    }

    @Test
    public void signUp_shouldThrowException_whenUserAlreadyExists() {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();

        doThrow(new UserAlreadyExistsException("User already exists"))
                .when(userService).throwIfUserExistsByEmail(authRequest.email());

        assertThrows(UserAlreadyExistsException.class, () -> authService.signUp(authRequest));

        verify(userService).throwIfUserExistsByEmail(authRequest.email());
        verify(userService, never()).save(any(User.class));
    }

    @Test
    public void signIn_shouldSuccessfullyAuthorizeUser() {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();
        User user = UserDataBuilder.buildUserWithRequiredFields()
                .status(UserStatus.ACTIVE)
                .build();
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";

        when(userService.findByEmailOrThrow(authRequest.email())).thenReturn(user);
        when(passwordEncoder.matches(authRequest.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);

        authService.signIn(authRequest);

        verify(userService).findByEmailOrThrow(authRequest.email());
        verify(passwordEncoder).matches(authRequest.password(), user.getPassword());
        verify(jwtService).generateAccessToken(user);
        verify(jwtService).generateRefreshToken(user);
    }

    @Test
    public void signIn_shouldThrowException_whenUserNotFound() {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();

        doThrow(new UsernameNotFoundException("User not found."))
                .when(userService).findByEmailOrThrow(authRequest.email());

        assertThrows(UsernameNotFoundException.class, () -> userService.findByEmailOrThrow(authRequest.email()));

        verify(userService).findByEmailOrThrow(authRequest.email());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(any(User.class));
        verify(jwtService, never()).generateRefreshToken(any(User.class));
    }

    @Test
    public void refresh_shouldReturnTokensPair() {
        RefreshRequest refreshRequest = RefreshRequest.builder().build();
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        TokenPayload tokenPayload = TokenPayload.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .build();

        when(jwtUserDetailsService.retrieveUserDetails(refreshRequest.refreshToken()))
                .thenReturn(user);
        when(jwtUserDetailsService.generateTokenPayload(user)).thenReturn(tokenPayload);

        authService.refresh(refreshRequest);

        verify(jwtUserDetailsService).retrieveUserDetails(refreshRequest.refreshToken());
        verify(jwtUserDetailsService).generateTokenPayload(user);
    }

    @Test
    public void refresh_shouldThrowException_whenRefreshTokenNotValid() {
        RefreshRequest refreshRequest = RefreshRequest.builder().build();

        doThrow(new BadCredentialsException("Token not valid"))
                .when(jwtUserDetailsService).retrieveUserDetails(refreshRequest.refreshToken());

        assertThrows(BadCredentialsException.class, () ->
                jwtUserDetailsService.retrieveUserDetails(refreshRequest.refreshToken()));

        verify(jwtUserDetailsService).retrieveUserDetails(refreshRequest.refreshToken());
        verify(jwtUserDetailsService, never()).generateTokenPayload(any(User.class));
    }
}