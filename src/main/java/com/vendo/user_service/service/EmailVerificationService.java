package com.vendo.user_service.service;

import com.vendo.user_service.common.exception.OtpAlreadySentException;
import com.vendo.user_service.integration.kafka.producer.EmailVerificationEventProducer;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    private String generateOtp() {
        int maxSixDigitNumber = new Random().nextInt(1_000_000);
        return String.format("%06d", maxSixDigitNumber);
    }
}
