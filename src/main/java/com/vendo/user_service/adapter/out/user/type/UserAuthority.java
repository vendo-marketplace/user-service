package com.vendo.user_service.adapter.out.user.type;

import com.vendo.domain.user.common.type.UserRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum UserAuthority implements GrantedAuthority {

    INTERNAL(UserRole.INTERNAL),
    USER(UserRole.USER),
    ADMIN(UserRole.ADMIN);

    private final UserRole role;

    @Override
    public String getAuthority() {
        return role.name();
    }
}