package com.vendo.user_service.db.model;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.annotation.Adult;
import com.vendo.user_service.common.dto.AuditingEntity;
import com.vendo.user_service.security.common.type.UserAuthority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class User extends AuditingEntity implements UserDetails {

    @Id
    private String id;

    @NotNull(message = "Email is required.")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email.")
    private String email;

    private boolean emailVerified;

    @NotNull(message = "Role is required.")
    private UserAuthority role;

    @NotNull(message = "Status is required.")
    private UserStatus status;

    @NotNull(message = "Provider is required.")
    private ProviderType providerType;

    @NotNull(message = "Password is required.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$", message = "Invalid password. Should include minimum 8 characters, 1 uppercase character, 1 lowercase character, 1 special symbol.")
    private String password;

    @Adult(message = "User should be at least 18 years old.")
    private LocalDate birthDate;

    @Pattern(regexp = "^[A-ZА-ЯІЇЄҐ][a-zа-яіїєґ]+ [A-ZА-ЯІЇЄҐ][a-zа-яіїєґ]+$", message = "Full name must contain two words, each starting with an uppercase letter and followed by lowercase letters.")
    private String fullName;

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
