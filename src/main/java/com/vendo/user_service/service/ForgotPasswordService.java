package com.vendo.user_service.service;

import com.vendo.integration.redis.common.exception.RedisValueExpiredException;
import com.vendo.user_service.common.exception.PasswordRecoveryNotificationAlreadySentException;
import com.vendo.user_service.integration.kafka.producer.NotificationEventProducer;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.common.dto.ForgotPasswordRequest;
import com.vendo.user_service.integration.redis.common.dto.ResetPasswordRequest;
import com.vendo.user_service.integration.redis.common.dto.UpdateUserRequest;
import com.vendo.user_service.integration.redis.service.RedisService;
import com.vendo.user_service.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final RedisService redisService;

    private final PasswordEncoder passwordEncoder;

    private final NotificationEventProducer notificationEventProducer;

    private final RedisProperties redisProperties;

    private final UserService userService;

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

        notificationEventProducer.sendRecoveryPasswordNotificationEvent(token);
    }

    public void resetPassword(String token, ResetPasswordRequest resetPasswordRequest) {
        String resetPasswordTokenPrefix = redisProperties.getResetPassword().getPrefixes().getTokenPrefix();
        String resetPasswordEmailPrefix = redisProperties.getResetPassword().getPrefixes().getEmailPrefix();

        String email = redisService.getValue(resetPasswordTokenPrefix + token)
                .orElseThrow(() -> new RedisValueExpiredException("Password recovery token has expired"));

        User user = userService.findByEmailOrThrow(email);

        userService.update(user.getId(), UpdateUserRequest.builder()
                .password(passwordEncoder.encode(resetPasswordRequest.password()))
                .build());

        redisService.deleteValues(resetPasswordTokenPrefix + token, resetPasswordEmailPrefix + email);
    }

}
