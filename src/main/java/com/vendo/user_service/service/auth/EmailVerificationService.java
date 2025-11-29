package com.vendo.user_service.service.auth;

import com.vendo.integration.kafka.event.EmailOtpEvent;
import com.vendo.user_service.model.User;
import com.vendo.user_service.service.otp.EmailOtpService;
import com.vendo.user_service.service.user.UserService;
import com.vendo.user_service.system.redis.common.dto.ValidateRequest;
import com.vendo.user_service.system.redis.common.namespace.otp.EmailVerificationOtpNamespace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.vendo.integration.kafka.event.EmailOtpEvent.OtpEventType.EMAIL_VERIFICATION;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserService userService;

    private final EmailOtpService emailOtpService;

    private final EmailVerificationOtpNamespace emailVerificationOtpNamespace;

    public void sendOtp(String email) {
        userService.findByEmailOrThrow(email);

        EmailOtpEvent event = EmailOtpEvent.builder()
                .email(email)
                .otpEventType(EMAIL_VERIFICATION)
                .build();
        emailOtpService.sendOtp(event, emailVerificationOtpNamespace);
    }

    public void resendOtp(String email) {
        userService.findByEmailOrThrow(email);

        EmailOtpEvent event = EmailOtpEvent.builder()
                .email(email)
                .otpEventType(EMAIL_VERIFICATION)
                .build();
        emailOtpService.resendOtp(event, emailVerificationOtpNamespace);
    }

    public void validate(String otp, ValidateRequest validateRequest) {
        User user = userService.findByEmailOrThrow(validateRequest.email());

        emailOtpService.verifyOtp(otp, validateRequest.email(), emailVerificationOtpNamespace);

        userService.update(user.getId(), User.builder()
                .emailVerified(true)
                .build());
    }

}
