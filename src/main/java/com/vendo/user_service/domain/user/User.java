package com.vendo.user_service.domain.user;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.domain.user.service.UserActivityView;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class User implements UserActivityView {

    private String email;
    private boolean emailVerified;
    private UserStatus status;
    private ProviderType providerType;
    private String password;
    private LocalDate birthDate;
    private String fullName;

    @Override
    public UserStatus getStatus() {
        return this.status;
    }

    @Override
    public Boolean getEmailVerified() {
        return this.emailVerified;
    }
}
