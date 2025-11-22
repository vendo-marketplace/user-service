package com.vendo.user_service.model;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.type.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

@Data
@Builder
public class User implements UserDetails {

    @Id
    private String id;

    @NotNull(message = "Email is required.")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email.")
    private String email;

    @NotNull(message = "Role is required.")
    private UserRole role;

    @NotNull(message = "Status is required.")
    private UserStatus status;

    @NotNull(message = "Provider is required.")
    private ProviderType providerType;

    @NotNull(message = "Password is required.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$", message = "Invalid password. Should include minimum 8 characters, 1 uppercase character, 1 lowercase character, 1 special symbol.")
    private String password;

    @DateTimeFormat(pattern = "dd/MM/yyyy")
    @Past(message = "Birth date must be in the past.")
    private LocalDate birthDate;

    @Pattern(regexp = "^[A-Z][A-Za-z'-]{1,49}(?: [A-Z][A-Za-z'-]{1,49}){1,2}$",
            message = "Invalid full name. Should contain 2-3 words, each starting with capital letter.")
    private String fullName;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

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
