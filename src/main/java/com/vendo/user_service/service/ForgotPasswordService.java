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

import java.util.Random;

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
        RedisProperties.ResetPassword resetProperties = redisProperties.getResetPassword();
        RedisProperties.ResetPassword.Prefixes resetPrefixes = resetProperties.getPrefixes();

        if (redisService.hasActiveKey(resetPrefixes.getEmailPrefix() + forgotPasswordRequests.email())) {
            throw new PasswordRecoveryNotificationAlreadySentException("Password recovery notification has already sent");
        }

        User user = userService.findByEmailOrThrow(forgotPasswordRequests.email());
        String otpCode = generateOtpCode();

        redisService.saveValue(resetPrefixes.getOtpPrefix() + otpCode, user.getEmail(), resetProperties.getTtl());
        redisService.saveValue(resetPrefixes.getEmailPrefix() + user.getEmail(), otpCode, resetProperties.getTtl());

        notificationEventProducer.sendRecoveryPasswordNotificationEvent(otpCode);
    }

    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        RedisProperties.ResetPassword resetProperties = redisProperties.getResetPassword();
        RedisProperties.ResetPassword.Prefixes resetPrefixes = resetProperties.getPrefixes();

        String email = redisService.getValue(resetPrefixes.getOtpPrefix() + resetPasswordRequest.otp())
                .orElseThrow(() -> new RedisValueExpiredException("Password recovery otp has expired"));

        User user = userService.findByEmailOrThrow(email);

        userService.update(user.getId(), UpdateUserRequest.builder()
                .password(passwordEncoder.encode(resetPasswordRequest.password()))
                .build());

        redisService.deleteValues(resetPrefixes.getOtpPrefix() + resetPasswordRequest.otp(), resetPrefixes.getEmailPrefix() + email);
    }

    private String generateOtpCode() {
        int maxSixDigitNumber = new Random().nextInt(1_000_000);
        return String.format("%06d", maxSixDigitNumber);
    }

}
