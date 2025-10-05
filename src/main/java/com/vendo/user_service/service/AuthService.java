package com.vendo.user_service.service;

import com.vendo.security.common.exception.AccessDeniedException;
import com.vendo.user_service.common.exception.PasswordRecoveryNotificationAlreadySentException;
import com.vendo.user_service.common.exception.RedisValueExpiredException;
import com.vendo.user_service.integration.kafka.producer.NotificationEventProducer;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.common.dto.ForgotPasswordRequest;
import com.vendo.user_service.integration.redis.common.dto.ResetPasswordRequest;
import com.vendo.user_service.integration.redis.common.dto.UpdateUserRequest;
import com.vendo.user_service.integration.redis.service.RedisService;
import com.vendo.user_service.model.User;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.common.type.UserStatus;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.security.service.JwtUserDetailsService;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    private final JwtService jwtService;

    private final RedisService redisService;

    private final PasswordEncoder passwordEncoder;

    private final NotificationEventProducer notificationEventProducer;

    private final JwtUserDetailsService jwtUserDetailsService;

    private final RedisProperties redisProperties;

    public AuthResponse signIn(AuthRequest authRequest) {
        User user = userService.findByEmailOrThrow(authRequest.getEmail());

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccessDeniedException("User is blocked");
        }
        matchPasswordsOrThrow(authRequest.getPassword(), user.getPassword());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

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

    public AuthResponse refresh(RefreshRequest refreshRequest) {
        UserDetails userDetails = jwtUserDetailsService.retrieveUserDetails(refreshRequest.getRefreshToken());
        TokenPayload tokenPayload = jwtUserDetailsService.generateTokenPayload(userDetails);

        return AuthResponse.builder()
                .accessToken(tokenPayload.getAccessToken())
                .refreshToken(tokenPayload.getRefreshToken())
                .build();
    }

    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequests) {
        if (redisService.hasActiveKey(redisProperties.getResetPassword().getPrefixes().getEmailPrefix() + forgotPasswordRequests.getEmail())) {
            throw new PasswordRecoveryNotificationAlreadySentException("Password recovery notification has already sent");
        }

        User user = userService.findByEmailOrThrow(forgotPasswordRequests.getEmail());
        String token = String.valueOf(UUID.randomUUID());

        String resetPasswordTokenPrefix = redisProperties.getResetPassword().getPrefixes().getTokenPrefix();
        String resetPasswordEmailPrefix = redisProperties.getResetPassword().getPrefixes().getEmailPrefix();
        redisService.saveValue(resetPasswordTokenPrefix + token, user.getEmail(), redisProperties.getResetPassword().getTtl());
        redisService.saveValue(resetPasswordEmailPrefix + user.getEmail(), token, redisProperties.getResetPassword().getTtl());

        notificationEventProducer.sendRecoveryPasswordNotificationEvent(token);
    }

    public void resetPassword(String token, ResetPasswordRequest resetPasswordRequest) {
        String email = redisService.getValue(redisProperties.getResetPassword().getPrefixes().getTokenPrefix() + token)
                .orElseThrow(() -> new RedisValueExpiredException("Notification token has expired"));

        User user = userService.findByEmailOrThrow(email);

        userService.update(user.getId(), UpdateUserRequest.builder()
                .password(passwordEncoder.encode(resetPasswordRequest.getPassword()))
                .build());
    }

    private void matchPasswordsOrThrow(String rawPassword, String encodedPassword) {
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        if (!matches) {
            throw new BadCredentialsException("Wrong credentials");
        }
    }
}
