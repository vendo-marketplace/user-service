package com.vendo.user_service.service.user;

import com.vendo.integration.kafka.event.EmailOtpEvent;
import com.vendo.integration.redis.common.exception.OtpExpiredException;
import com.vendo.user_service.system.redis.common.dto.ResetPasswordRequest;
import com.vendo.user_service.system.redis.common.namespace.otp.PasswordRecoveryOtpNamespace;
import com.vendo.user_service.system.redis.service.RedisService;
import com.vendo.user_service.model.User;
import com.vendo.user_service.service.otp.EmailOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.vendo.integration.kafka.event.EmailOtpEvent.OtpEventType.PASSWORD_RECOVERY;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final RedisService redisService;

    private final PasswordEncoder passwordEncoder;

    private final PasswordRecoveryOtpNamespace passwordRecoveryOtpNamespace;

    private final UserService userService;

    private final EmailOtpService emailOtpService;

    public void forgotPassword(String email) {
        userService.findByEmailOrThrow(email);

        EmailOtpEvent event = EmailOtpEvent.builder()
                .email(email)
                .otpEventType(PASSWORD_RECOVERY)
                .build();
        emailOtpService.sendOtp(event, passwordRecoveryOtpNamespace);
    }

    public void resetPassword(String otp, ResetPasswordRequest resetPasswordRequest) {
        String email = redisService.getValue(passwordRecoveryOtpNamespace.getOtp().buildPrefix(String.valueOf(otp)))
                .orElseThrow(() -> new OtpExpiredException("Otp session expired."));

        User user = userService.findByEmailOrThrow(email);
        userService.update(user.getId(), User.builder()
                .password(passwordEncoder.encode(resetPasswordRequest.password()))
                .build());

        redisService.deleteValues(
                passwordRecoveryOtpNamespace.getOtp().buildPrefix(String.valueOf(otp)),
                passwordRecoveryOtpNamespace.getEmail().buildPrefix(email));
    }

    public void resendOtp(String email) {
        userService.findByEmailOrThrow(email);

        EmailOtpEvent event = EmailOtpEvent.builder()
                .email(email)
                .otpEventType(PASSWORD_RECOVERY)
                .build();
        emailOtpService.resendOtp(event, passwordRecoveryOtpNamespace);
    }
}
