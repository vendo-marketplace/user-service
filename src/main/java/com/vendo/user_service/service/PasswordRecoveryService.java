package com.vendo.user_service.service;

import com.vendo.integration.redis.common.exception.RedisValueExpiredException;
import com.vendo.user_service.common.exception.OtpAlreadySentException;
import com.vendo.user_service.common.exception.OtpTooManyRequestsException;
import com.vendo.user_service.integration.kafka.producer.PasswordRecoveryEventProducer;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.common.dto.ResetPasswordRequest;
import com.vendo.user_service.integration.redis.common.dto.UpdateUserRequest;
import com.vendo.user_service.integration.redis.service.RedisService;
import com.vendo.user_service.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final RedisService redisService;

    private final PasswordEncoder passwordEncoder;

    private final PasswordRecoveryEventProducer passwordRecoveryEventProducer;

    private final RedisProperties redisProperties;

    private final UserService userService;

    public void forgotPassword(String email) {
        userService.findByEmailOrThrow(email);
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();

        if (redisService.hasActiveKey(recoveryProperties.getEmail().buildPrefix(email))) {
            throw new OtpAlreadySentException("Otp has already sent to email");
        }

        String otp = generateOtp();
        redisService.saveValue(recoveryProperties.getOtp().buildPrefix(otp), email, recoveryProperties.getOtp().getTtl());
        redisService.saveValue(recoveryProperties.getEmail().buildPrefix(email), otp, recoveryProperties.getEmail().getTtl());

        passwordRecoveryEventProducer.sendRecoveryPasswordEvent(email);
    }

    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();

        String email = redisService.getValue(recoveryProperties.getOtp().buildPrefix(String.valueOf(resetPasswordRequest.otp())))
                .orElseThrow(() -> new RedisValueExpiredException("Otp has expired"));

        User user = userService.findByEmailOrThrow(email);
        userService.update(user.getId(), UpdateUserRequest.builder()
                .password(passwordEncoder.encode(resetPasswordRequest.password()))
                .build());

        redisService.deleteValues(
                recoveryProperties.getOtp().buildPrefix(String.valueOf(resetPasswordRequest.otp())),
                recoveryProperties.getEmail().buildPrefix(email));
    }

    public void resendOtp(String email) {
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        userService.findByEmailOrThrow(email);

        Optional<String> attempts = redisService.getValue(recoveryProperties.getAttempts().buildPrefix(email));
        increaseResendAttemptsOrThrow(email, attempts);

        Optional<String> otp = redisService.getValue(recoveryProperties.getEmail().buildPrefix(email));
        resendOrGenerateOtp(email, otp);
    }

    private void increaseResendAttemptsOrThrow(String email, Optional<String> attempts) {
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        int attempt = attempts.map(Integer::parseInt).orElse(0);

        if(attempt == 3) {
            throw new OtpTooManyRequestsException("User reached maximum attempts for resending otp code");
        }

        redisService.saveValue(
                recoveryProperties.getAttempts().buildPrefix(email),
                String.valueOf(attempt + 1),
                recoveryProperties.getAttempts().getTtl());
    }

    private void resendOrGenerateOtp(String email, Optional<String> otp) {
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();

        if (otp.isEmpty()) {
            redisService.saveValue(recoveryProperties.getEmail().buildPrefix(email), generateOtp(), recoveryProperties.getOtp().getTtl());
        }

        passwordRecoveryEventProducer.sendRecoveryPasswordEvent(email);
    }

    private String generateOtp() {
        int maxSixDigitNumber = new Random().nextInt(1_000_000);
        return String.format("%06d", maxSixDigitNumber);
    }
}
