package com.vendo.user_service.security.common.type;

import com.vendo.domain.user.common.type.UserRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum UserAuthority implements GrantedAuthority {

    USER(UserRole.USER),
    ADMIN(UserRole.ADMIN);

    private final UserRole role;

    @Override
    public String getAuthority() {
        return role.name();
    }
}
