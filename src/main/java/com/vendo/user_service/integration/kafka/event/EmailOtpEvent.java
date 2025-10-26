package com.vendo.user_service.integration.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// TODO move to common
public class EmailOtpEvent {

    private String otp;

    private String email;

    private OtpEventType otpEventType;

    public enum OtpEventType {

        EMAIL_VERIFICATION,

        PASSWORD_RECOVERY

    }
}
