package com.vendo.user_service.service.auth;

import com.vendo.integration.kafka.event.EmailOtpEvent;
import com.vendo.user_service.db.command.UserCommandService;
import com.vendo.user_service.db.model.User;
import com.vendo.user_service.db.query.UserQueryService;
import com.vendo.user_service.service.otp.EmailOtpService;
import com.vendo.user_service.system.redis.common.dto.ValidateRequest;
import com.vendo.user_service.system.redis.common.namespace.otp.EmailVerificationOtpNamespace;
import com.vendo.user_service.web.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.vendo.integration.kafka.event.EmailOtpEvent.OtpEventType.EMAIL_VERIFICATION;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserQueryService userQueryService;

    private final UserCommandService userCommandService;

    private final EmailOtpService emailOtpService;

    private final EmailVerificationOtpNamespace emailVerificationOtpNamespace;

    public void sendOtp(String email) {
        userQueryService.loadUserByUsername(email);

        EmailOtpEvent event = EmailOtpEvent.builder()
                .email(email)
                .otpEventType(EMAIL_VERIFICATION)
                .build();
        emailOtpService.sendOtp(event, emailVerificationOtpNamespace);
    }

    public void resendOtp(String email) {
        userQueryService.loadUserByUsername(email);

        EmailOtpEvent event = EmailOtpEvent.builder()
                .email(email)
                .otpEventType(EMAIL_VERIFICATION)
                .build();
        emailOtpService.resendOtp(event, emailVerificationOtpNamespace);
    }

    public void validate(String otp, ValidateRequest validateRequest) {
        User user = userQueryService.loadUserByUsername(validateRequest.email());

        emailOtpService.verifyOtp(otp, validateRequest.email(), emailVerificationOtpNamespace);

        userCommandService.update(user.getId(), UserUpdateRequest.builder()
                .emailVerified(true)
                .build());
    }

}
