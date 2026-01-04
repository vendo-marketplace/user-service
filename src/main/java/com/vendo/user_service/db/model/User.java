package com.vendo.user_service.db.model;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.domain.user.service.UserActivityView;
import com.vendo.user_service.common.dto.AuditingEntity;
import com.vendo.user_service.security.common.type.UserAuthority;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

@Data
@Document
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends AuditingEntity implements UserDetails, UserActivityView {

    @Id
    private String id;

    private String email;

    private boolean emailVerified;

    private UserAuthority role;

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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(role);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

}
