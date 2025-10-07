package com.vendo.user_service.integration.kafka.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordRecoveryEvent {

    private String email;

    private String token;

}
