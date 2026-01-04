package com.vendo.user_service.service.auth;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.domain.user.service.UserActivityPolicy;
import com.vendo.security.common.exception.InvalidTokenException;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.db.command.UserCommandService;
import com.vendo.user_service.db.model.User;
import com.vendo.user_service.db.query.UserQueryService;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.common.helper.JwtHelper;
import com.vendo.user_service.security.common.type.UserAuthority;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.service.user.UserActivityValidationService;
import com.vendo.user_service.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.vendo.security.common.constants.AuthConstants.BEARER_PREFIX;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserCommandService userCommandService;

    private final UserQueryService userQueryService;

    private final UserActivityValidationService userActivityValidationService;

    private final JwtService jwtService;

    private final JwtHelper jwtHelper;

    private final PasswordEncoder passwordEncoder;

    public AuthResponse signIn(AuthRequest authRequest) {
        User user = userQueryService.loadUserByUsername(authRequest.email());

        UserActivityPolicy.validateActivity(user);

        matchPasswordsOrThrow(authRequest.password(), user.getPassword());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void signUp(AuthRequest authRequest) {
        userQueryService.findByEmail(authRequest.email()).ifPresent(user -> {
            throw new UserAlreadyExistsException("User already exists.");
        });

        String encodedPassword = passwordEncoder.encode(authRequest.password());

        userCommandService.save(User.builder()
                .email(authRequest.email())
                .role(UserAuthority.USER)
                .status(UserStatus.INCOMPLETE)
                .providerType(ProviderType.LOCAL)
                .password(encodedPassword)
                .emailVerified(false)
                .build());
    }

    public void completeAuth(String email, CompleteAuthRequest completeAuthRequest) {
        User user = userQueryService.loadUserByUsername(email);

        userActivityValidationService.validateBeforeActivation(user);

        userCommandService.update(user.getId(), UserUpdateRequest.builder()
                .status(UserStatus.ACTIVE)
                .fullName(completeAuthRequest.fullName())
                .birthDate(completeAuthRequest.birthDate())
                .build());
    }

    public AuthResponse refresh(RefreshRequest refreshRequest) {
        // TODO this logic shouldn't be here
        if (!refreshRequest.refreshToken().startsWith(BEARER_PREFIX)) {
            throw new InvalidTokenException("Invalid token.");
        }
        String token = refreshRequest.refreshToken().substring(BEARER_PREFIX.length());
        String email = jwtHelper.extractAllClaims(token).getSubject();

        User user = userQueryService.loadUserByUsername(email);
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
}
