package com.vendo.user_service.integration.redis.common.dto;

import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.common.type.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UpdateUserRequest {

    private String email;

    private UserRole role;

    private UserStatus status;

    private String password;

    private LocalDate birthDate;

    private String fullName;

}
