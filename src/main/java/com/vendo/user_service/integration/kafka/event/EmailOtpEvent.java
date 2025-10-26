package com.vendo.user_service.integration.kafka.event;

import com.vendo.user_service.integration.kafka.common.type.OtpEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailOtpEvent {

    private String otp;

    private String email;

    private OtpEventType otpEventType;

}
