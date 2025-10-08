package com.vendo.user_service.service;

import com.vendo.security.common.exception.AccessDeniedException;
import com.vendo.user_service.common.exception.PasswordRecoveryNotificationAlreadySentException;
import com.vendo.user_service.common.exception.RedisValueExpiredException;
import com.vendo.user_service.integration.kafka.common.dto.PasswordRecoveryEvent;
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
        User user = userService.findByEmailOrThrow(authRequest.email());

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccessDeniedException("User is blocked");
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
        userService.throwIfUserExistsByEmail(authRequest.email());
        String encodedPassword = passwordEncoder.encode(authRequest.password());

        userService.save(User.builder()
                        .email(authRequest.email())
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
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

    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequests) {
        String resetPasswordEmailPrefix = redisProperties.getResetPassword().getPrefixes().getEmailPrefix();
        String resetPasswordTokenPrefix = redisProperties.getResetPassword().getPrefixes().getTokenPrefix();
        long resetPasswordTtl = redisProperties.getResetPassword().getTtl();

        if (redisService.hasActiveKey(resetPasswordEmailPrefix + forgotPasswordRequests.email())) {
            throw new PasswordRecoveryNotificationAlreadySentException("Password recovery notification has already sent");
        }

        User user = userService.findByEmailOrThrow(forgotPasswordRequests.email());
        String token = String.valueOf(UUID.randomUUID());

        redisService.saveValue(resetPasswordTokenPrefix + token, user.getEmail(), resetPasswordTtl);
        redisService.saveValue(resetPasswordEmailPrefix + user.getEmail(), token, resetPasswordTtl);

        PasswordRecoveryEvent passwordRecoveryEvent = PasswordRecoveryEvent.builder()
                .email(user.getEmail())
                .token(token)
                .build();
        notificationEventProducer.sendRecoveryPasswordNotificationEvent(passwordRecoveryEvent);
    }

    public void resetPassword(String token, ResetPasswordRequest resetPasswordRequest) {
        String resetPasswordTokenPrefix = redisProperties.getResetPassword().getPrefixes().getTokenPrefix();
        String resetPasswordEmailPrefix = redisProperties.getResetPassword().getPrefixes().getEmailPrefix();

        String email = redisService.getValue(resetPasswordTokenPrefix + token)
                .orElseThrow(() -> new RedisValueExpiredException("Notification token has expired"));

        User user = userService.findByEmailOrThrow(email);

        userService.update(user.getId(), UpdateUserRequest.builder()
                .password(passwordEncoder.encode(resetPasswordRequest.password()))
                .build());

        redisService.deleteValue(resetPasswordTokenPrefix + token);
        redisService.deleteValue(resetPasswordEmailPrefix + email);
    }

    private void matchPasswordsOrThrow(String rawPassword, String encodedPassword) {
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        if (!matches) {
            throw new BadCredentialsException("Wrong credentials");
        }
    }
}
