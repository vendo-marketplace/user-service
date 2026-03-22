package com.vendo.user_service.domain.user;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserRole;
import com.vendo.user_lib.type.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String email;
    private boolean emailVerified;
    private UserRole role;
    private UserStatus status;
    private ProviderType providerType;
    private String password;
    private LocalDate birthDate;
    private String fullName;

}