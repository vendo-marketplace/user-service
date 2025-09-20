package com.vendo.user_service.service;

import com.vendo.user_service.builder.AuthRequestDataBuilder;
import com.vendo.user_service.builder.UserDataBuilder;
import com.vendo.user_service.exception.UserAlreadyExistsException;
import com.vendo.user_service.model.User;
import com.vendo.user_service.security.token.JwtService;
import com.vendo.user_service.security.token.JwtUserDetailsService;
import com.vendo.user_service.security.token.TokenPayload;
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

        doNothing().when(userService).throwIfUserExistsByEmail(authRequest.getEmail());
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn(encodedPassword);

        authService.signUp(authRequest);

        verify(userService).throwIfUserExistsByEmail(authRequest.getEmail());
        verify(passwordEncoder).encode(authRequest.getPassword());
        verify(userService).save(argThat(user ->
                        user.getEmail().equals(authRequest.getEmail()) &&
                        user.getPassword().equals(encodedPassword))
        );
    }

    @Test
    public void signUp_shouldThrowException_whenUserAlreadyExists() {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();

        doThrow(new UserAlreadyExistsException("User already exists"))
                .when(userService).throwIfUserExistsByEmail(authRequest.getEmail());

        assertThrows(UserAlreadyExistsException.class, () -> authService.signUp(authRequest));

        verify(userService).throwIfUserExistsByEmail(authRequest.getEmail());
        verify(userService, never()).save(any(User.class));
    }

    @Test
    public void signIn_shouldSuccessfullyAuthorizeUser() {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";

        when(userService.findByEmailOrThrow(authRequest.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(authRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);

        authService.signIn(authRequest);

        verify(userService).findByEmailOrThrow(authRequest.getEmail());
        verify(passwordEncoder).matches(authRequest.getPassword(), user.getPassword());
        verify(jwtService).generateAccessToken(user);
        verify(jwtService).generateRefreshToken(user);
    }

    @Test
    public void signIn_shouldThrowException_whenUserNotFound() {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();

        doThrow(new UsernameNotFoundException("User not found"))
                .when(userService).findByEmailOrThrow(authRequest.getEmail());

        assertThrows(UsernameNotFoundException.class, () -> userService.findByEmailOrThrow(authRequest.getEmail()));

        verify(userService).findByEmailOrThrow(authRequest.getEmail());
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

        when(jwtUserDetailsService.getUserDetailsIfTokenValidOrThrow(refreshRequest.getRefreshToken()))
                .thenReturn(user);
        when(jwtUserDetailsService.generateTokenPayload(user)).thenReturn(tokenPayload);

        authService.refresh(refreshRequest);

        verify(jwtUserDetailsService).getUserDetailsIfTokenValidOrThrow(refreshRequest.getRefreshToken());
        verify(jwtUserDetailsService).generateTokenPayload(user);
    }

    @Test
    public void refresh_shouldThrowException_whenRefreshTokenNotValid() {
        RefreshRequest refreshRequest = RefreshRequest.builder().build();

        doThrow(new BadCredentialsException("Token not valid"))
                .when(jwtUserDetailsService).getUserDetailsIfTokenValidOrThrow(refreshRequest.getRefreshToken());

        assertThrows(BadCredentialsException.class, () ->
                jwtUserDetailsService.getUserDetailsIfTokenValidOrThrow(refreshRequest.getRefreshToken()));

        verify(jwtUserDetailsService).getUserDetailsIfTokenValidOrThrow(refreshRequest.getRefreshToken());
        verify(jwtUserDetailsService, never()).generateTokenPayload(any(User.class));
    }
}