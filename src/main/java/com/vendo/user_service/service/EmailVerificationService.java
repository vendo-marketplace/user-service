package com.vendo.user_service.service;

import com.vendo.integration.redis.common.exception.RedisValueExpiredException;
import com.vendo.user_service.common.exception.OtpAlreadySentException;
import com.vendo.user_service.common.exception.OtpTooManyRequestsException;
import com.vendo.user_service.integration.kafka.producer.EmailVerificationEventProducer;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisService redisService;

    private final EmailVerificationEventProducer emailVerificationEventProducer;

    private final RedisProperties redisProperties;

    private final UserService userService;

    public void sendVerificationCode(String email) {
        userService.findByEmailOrThrow(email);
        RedisProperties.EmailVerification verificationProperties = redisProperties.getEmailVerification();

        if (redisService.hasActiveKey(verificationProperties.getEmail().buildPrefix(email))) {
            throw new OtpAlreadySentException("Otp has already sent to email.");
        }

        String otp = generateOtp();
        redisService.saveValue(verificationProperties.getOtp().buildPrefix(otp), email, verificationProperties.getOtp().getTtl());
        redisService.saveValue(verificationProperties.getEmail().buildPrefix(email), otp, verificationProperties.getEmail().getTtl());

        emailVerificationEventProducer.sendEmailVerificationEvent(email);
    }

    public void resendVerificationCode(String email) {
        RedisProperties.EmailVerification verificationProperties = redisProperties.getEmailVerification();

        userService.findByEmailOrThrow(email);
        redisService.getValue(verificationProperties.getEmail().buildPrefix(email))
                .orElseThrow(() -> new RedisValueExpiredException("Verification session expired."));

        Optional<String> attempts = redisService.getValue(verificationProperties.getAttempts().buildPrefix(email));
        increaseResendAttemptsOrThrow(email, attempts);

        Optional<String> otp = redisService.getValue(verificationProperties.getEmail().buildPrefix(email));
        resendOrGenerateOtp(email, otp);
    }

    private void increaseResendAttemptsOrThrow(String email, Optional<String> attempts) {
        RedisProperties.EmailVerification verificationProperties = redisProperties.getEmailVerification();
        int attempt = attempts.map(Integer::parseInt).orElse(0);

        if (attempt >= 3) {
            throw new OtpTooManyRequestsException("Reached maximum attempts for resending otp code.");
        }

        redisService.saveValue(
                verificationProperties.getAttempts().buildPrefix(email),
                String.valueOf(attempt + 1),
                verificationProperties.getAttempts().getTtl());
    }

    private void resendOrGenerateOtp(String email, Optional<String> otp) {
        RedisProperties.EmailVerification verificationProperties = redisProperties.getEmailVerification();

        if (otp.isEmpty()) {
            redisService.saveValue(verificationProperties.getEmail().buildPrefix(email), generateOtp(), verificationProperties.getOtp().getTtl());
        }

        emailVerificationEventProducer.sendEmailVerificationEvent(email);
    }

    private String generateOtp() {
        int maxSixDigitNumber = new Random().nextInt(1_000_000);
        return String.format("%06d", maxSixDigitNumber);
    }
}
