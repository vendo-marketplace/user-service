package com.vendo.user_service.service.otp;

import com.vendo.integration.kafka.event.EmailOtpEvent;
import com.vendo.integration.redis.common.exception.OtpExpiredException;
import com.vendo.user_service.service.otp.common.exception.InvalidOtpException;
import com.vendo.user_service.service.otp.common.exception.OtpAlreadySentException;
import com.vendo.user_service.service.otp.common.exception.TooManyOtpRequestsException;
import com.vendo.user_service.system.kafka.producer.EmailOtpEventProducer;
import com.vendo.user_service.system.redis.common.namespace.otp.OtpNamespace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private final OtpGenerator otpGenerator;

    private final OtpStorage otpStorage;

    private final EmailOtpEventProducer emailOtpEventProducer;

    public void sendOtp(EmailOtpEvent event, OtpNamespace otpNamespace) {
        if (otpStorage.hasActiveKey(otpNamespace.getEmail().buildPrefix(event.getEmail()))) {
            throw new OtpAlreadySentException("Otp has already sent.");
        }

        String otp = otpGenerator.generate();
        event.setOtp(otp);

        otpStorage.saveValue(otpNamespace.getOtp().buildPrefix(otp), event.getEmail(), otpNamespace.getOtp().getTtl());
        otpStorage.saveValue(otpNamespace.getEmail().buildPrefix(event.getEmail()), otp, otpNamespace.getEmail().getTtl());

        emailOtpEventProducer.sendEmailOtpEvent(event);
    }

    public void resendOtp(EmailOtpEvent event, OtpNamespace otpNamespace) {
        otpStorage.getValue(otpNamespace.getEmail().buildPrefix(event.getEmail()))
                .orElseThrow(() -> new OtpExpiredException("Otp session expired."));

        increaseResendAttemptsOrThrow(event.getEmail(), otpNamespace);

        String otp = getOtpOrGenerate(event.getEmail(), otpNamespace);
        event.setOtp(otp);

        emailOtpEventProducer.sendEmailOtpEvent(event);
    }

    public String verifyOtpAndConsume(String otp, @Nullable String expectedEmail, OtpNamespace namespace) {
        String actualEmail = otpStorage.getValue(namespace.getOtp().buildPrefix(otp))
                .orElseThrow(() -> new OtpExpiredException("Otp session expired."));

        if (expectedEmail != null && !actualEmail.equals(expectedEmail)) {
            throw new InvalidOtpException("Invalid otp.");
        }

        otpStorage.deleteValues(
                namespace.getOtp().buildPrefix(otp),
                namespace.getEmail().buildPrefix(actualEmail),
                namespace.getAttempts().buildPrefix(actualEmail)
        );

        return actualEmail;
    }

    private void increaseResendAttemptsOrThrow(String email, OtpNamespace otpNamespace) {
        Optional<String> attempts = otpStorage.getValue(otpNamespace.getAttempts().buildPrefix(email));
        int attempt = attempts.map(Integer::parseInt).orElse(0);

        if (attempt >= 3) {
            throw new TooManyOtpRequestsException("Reached maximum attempts.");
        }

        otpStorage.saveValue(
                otpNamespace.getAttempts().buildPrefix(email),
                String.valueOf(attempt + 1),
                otpNamespace.getAttempts().getTtl());
    }

    private String getOtpOrGenerate(String email, OtpNamespace otpNamespace) {
        Optional<String> otp = otpStorage.getValue(otpNamespace.getEmail().buildPrefix(email));

        if (otp.isEmpty()) {
            otp = otp.map(o -> otpGenerator.generate());
            otpStorage.saveValue(otpNamespace.getEmail().buildPrefix(email), otp.get(), otpNamespace.getOtp().getTtl());
        }

        return otp.get();
    }
}
