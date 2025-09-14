package com.vendo.user_service.service;

import com.vendo.user_service.exception.WrongCredentialsException;
import com.vendo.user_service.model.User;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.common.type.UserStatus;
import com.vendo.user_service.security.JwtService;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    public void signUp(AuthRequest authRequest) {
        userService.throwIfUserExistsByEmail(authRequest.getEmail());
        String encodedPassword = passwordEncoder.encode(authRequest.getPassword());

        userService.save(User.builder()
                        .email(authRequest.getEmail())
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .password(encodedPassword)
                .build());
    }

    public AuthResponse signIn(AuthRequest authRequest) {
        User user = userService.findByEmailOrThrow(authRequest.getEmail());
        matchPasswordsOrThrow(authRequest.getPassword(), user.getPassword());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // TODO refresh endpoint
    public AuthResponse refresh() {
        return AuthResponse.builder().build();
    }

    public void matchPasswordsOrThrow(String rawPassword, String encodedPassword) {
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        if (!matches) {
            throw new WrongCredentialsException("Wrong credentials");
        }
    }

}
