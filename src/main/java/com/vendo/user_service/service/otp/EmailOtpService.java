package com.vendo.user_service.service.otp;

import com.vendo.integration.kafka.event.EmailOtpEvent;
import com.vendo.integration.redis.common.exception.OtpExpiredException;
import com.vendo.user_service.common.exception.InvalidOtpException;
import com.vendo.user_service.common.exception.OtpAlreadySentException;
import com.vendo.user_service.common.exception.TooManyOtpRequestsException;
import com.vendo.user_service.system.kafka.producer.EmailOtpEventProducer;
import com.vendo.user_service.system.redis.common.namespace.otp.OtpNamespace;
import com.vendo.user_service.system.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private final OtpGenerator otpGenerator;

    private final RedisService redisService;

    private final EmailOtpEventProducer emailOtpEventProducer;

    public void sendOtp(EmailOtpEvent event, OtpNamespace otpNamespace) {
        if (redisService.hasActiveKey(otpNamespace.getEmail().buildPrefix(event.getEmail()))) {
            throw new OtpAlreadySentException("Otp has already sent.");
        }

        String otp = otpGenerator.generateSixDigitOtp();
        event.setOtp(otp);

        redisService.saveValue(otpNamespace.getOtp().buildPrefix(otp), event.getEmail(), otpNamespace.getOtp().getTtl());
        redisService.saveValue(otpNamespace.getEmail().buildPrefix(event.getEmail()), otp, otpNamespace.getEmail().getTtl());

        emailOtpEventProducer.sendEmailOtpEvent(event);
    }

    public void resendOtp(EmailOtpEvent event,OtpNamespace otpNamespace) {
        redisService.getValue(otpNamespace.getEmail().buildPrefix(event.getEmail()))
                .orElseThrow(() -> new OtpExpiredException("Otp session expired."));

        increaseResendAttemptsOrThrow(event.getEmail(), otpNamespace);

        String otp = getOtpOrGenerate(event.getEmail(), otpNamespace);
        event.setOtp(otp);

        emailOtpEventProducer.sendEmailOtpEvent(event);
    }

    public void verifyOtp(String otp, String email, OtpNamespace otpNamespace) {
        String redisEmail = redisService.getValue(otpNamespace.getOtp().buildPrefix(otp))
                .orElseThrow(() -> new OtpExpiredException("Otp session expired."));

        if (!redisEmail.equals(email)) {
            throw new InvalidOtpException("Invalid otp.");
        }

        redisService.deleteValues(
                otpNamespace.getOtp().buildPrefix(otp),
                otpNamespace.getEmail().buildPrefix(email),
                otpNamespace.getAttempts().buildPrefix(email)
        );
    }

    private void increaseResendAttemptsOrThrow(String email, OtpNamespace otpNamespace) {
        Optional<String> attempts = redisService.getValue(otpNamespace.getAttempts().buildPrefix(email));
        int attempt = attempts.map(Integer::parseInt).orElse(0);

        if (attempt >= 3) {
            throw new TooManyOtpRequestsException("Reached maximum attempts.");
        }

        redisService.saveValue(
                otpNamespace.getAttempts().buildPrefix(email),
                String.valueOf(attempt + 1),
                otpNamespace.getAttempts().getTtl());
    }

    private String getOtpOrGenerate(String email, OtpNamespace otpNamespace) {
        Optional<String> otp = redisService.getValue(otpNamespace.getEmail().buildPrefix(email));

        if (otp.isEmpty()) {
            otp = otp.map(o -> otpGenerator.generateSixDigitOtp());
            redisService.saveValue(otpNamespace.getEmail().buildPrefix(email), otp.get(), otpNamespace.getOtp().getTtl());
        }

        return otp.get();
    }
}
