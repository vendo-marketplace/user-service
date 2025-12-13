package com.vendo.user_service.service.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.security.common.exception.InvalidTokenException;
import com.vendo.security.common.exception.UserBlockedException;
import com.vendo.security.common.exception.UserEmailNotVerifiedException;
import com.vendo.security.common.exception.UserIsUnactiveException;
import com.vendo.user_service.common.exception.UserAlreadyActivatedException;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.security.common.type.UserAuthority;
import com.vendo.user_service.model.User;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.common.helper.JwtHelper;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.service.user.UserService;
import com.vendo.user_service.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.vendo.security.common.constants.AuthConstants.BEARER_PREFIX;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    private final JwtService jwtService;

    private final JwtHelper jwtHelper;

    private final PasswordEncoder passwordEncoder;

    private final GoogleOAuthService googleOauthService;

    public AuthResponse signIn(AuthRequest authRequest) {
        User user = userService.loadUserByUsername(authRequest.email());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserIsUnactiveException("User is unactive.");
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
                .role(UserAuthority.USER)
                .status(UserStatus.INCOMPLETE)
                .providerType(ProviderType.LOCAL)
                .password(encodedPassword)
                .emailVerified(false)
                .build());
    }

    public void completeAuth(String email, CompleteAuthRequest completeAuthRequest) {
        User user = userService.loadUserByUsername(email);

        validateUserBeforeCompleteAuth(user);

        userService.update(user.getId(), UserUpdateRequest.builder()
                .status(UserStatus.ACTIVE)
                .fullName(completeAuthRequest.fullName())
                .birthDate(completeAuthRequest.birthDate())
                .build());
    }

    public AuthResponse refresh(RefreshRequest refreshRequest) {
        if (!refreshRequest.refreshToken().startsWith(BEARER_PREFIX)) {
            throw new InvalidTokenException("Invalid token.");
        }
        String token = refreshRequest.refreshToken().substring(BEARER_PREFIX.length());

        String email = jwtHelper.extractAllClaims(token).getSubject();
        User user = userService.loadUserByUsername(email);
        TokenPayload tokenPayload = jwtService.generateTokenPayload(user);

        return AuthResponse.builder()
                .accessToken(tokenPayload.accessToken())
                .refreshToken(tokenPayload.refreshToken())
                .build();
    }

    public AuthResponse googleAuth(GoogleAuthRequest googleAuthRequest) {
        GoogleIdToken.Payload payload = googleOauthService.verify(googleAuthRequest.idToken());

        User user = userService.findUserByEmailOrSave(payload.getEmail());

        if (user.getStatus() == UserStatus.INCOMPLETE) {
            userService.update(user.getId(), UserUpdateRequest.builder()
                    .status(UserStatus.ACTIVE)
                    .providerType(ProviderType.GOOGLE).build()
            );
        }

        TokenPayload tokenPayload = jwtService.generateTokenPayload(user);
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

    private void validateUserBeforeCompleteAuth(User user) {
        if (!user.isEmailVerified()) {
            throw new UserEmailNotVerifiedException("Your email is not verified.");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new UserBlockedException("Your account is blocked.");
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new UserAlreadyActivatedException("Your account is already activated.");
        }
    }

}
