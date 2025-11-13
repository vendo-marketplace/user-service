package com.vendo.user_service.service.user.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.security.common.exception.AccessDeniedException;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.common.type.Provider;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.model.User;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.security.service.JwtUserDetailsService;
import com.vendo.user_service.service.user.UserService;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.AuthResponse;
import com.vendo.user_service.web.dto.GoogleAuthRequest;
import com.vendo.user_service.web.dto.RefreshRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    private final JwtUserDetailsService jwtUserDetailsService;

    private final GoogleOauthService googleOauthService;

    public AuthResponse signIn(AuthRequest authRequest) {
        User user = userService.findByEmailOrThrow(authRequest.email());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User is unactive.");
        }
        matchPasswordsOrThrow(authRequest.password(), user.getPassword());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void signUp(AuthRequest authRequest) {
        userService.findByEmail(authRequest.email()).ifPresent(user -> {
            throw new UserAlreadyExistsException("User already exists.");
        });

        String encodedPassword = passwordEncoder.encode(authRequest.password());

        userService.save(User.builder()
                .email(authRequest.email())
                .role(UserRole.USER)
                .status(UserStatus.INCOMPLETE)
                .provider(Provider.LOCAL)
                .password(encodedPassword)
                .build());
    }

    public AuthResponse refresh(RefreshRequest refreshRequest) {
        UserDetails userDetails = jwtUserDetailsService.retrieveUserDetails(refreshRequest.refreshToken());
        TokenPayload tokenPayload = jwtUserDetailsService.generateTokenPayload(userDetails);

        return AuthResponse.builder()
                .accessToken(tokenPayload.accessToken())
                .refreshToken(tokenPayload.refreshToken())
                .build();
    }

    public AuthResponse googleAuth(GoogleAuthRequest googleAuthRequest) {
        GoogleIdToken.Payload payload = googleOauthService.verify(googleAuthRequest.idToken());

        User user = userService.findUserByEmailOrSave(payload.getEmail());
        updateUserGoogleAuthActivity(user);

        TokenPayload tokenPayload = jwtUserDetailsService.generateTokenPayload(user);
        return AuthResponse.builder()
                .accessToken(tokenPayload.accessToken())
                .refreshToken(tokenPayload.refreshToken())
                .build();
    }

    private void matchPasswordsOrThrow(String rawPassword, String encodedPassword) {
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        if (!matches) {
            throw new BadCredentialsException("Wrong credentials");
        }
    }

    private void updateUserGoogleAuthActivity(User user) {
        if (user.getStatus() == UserStatus.INCOMPLETE) {
            user.setStatus(UserStatus.ACTIVE);
        }

        user.setProvider(Provider.GOOGLE);

        userService.update(user.getId(), user);
    }
}
