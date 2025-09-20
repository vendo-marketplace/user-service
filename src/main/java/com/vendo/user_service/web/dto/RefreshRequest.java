package com.vendo.user_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

}
