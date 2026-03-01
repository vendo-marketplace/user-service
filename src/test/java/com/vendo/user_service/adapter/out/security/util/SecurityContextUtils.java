package com.vendo.user_service.adapter.out.security.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class SecurityContextUtils {

    public static Authentication initAuth(String subject, Collection<? extends GrantedAuthority> authorities) {
        return new UsernamePasswordAuthenticationToken(
                subject,
                null,
                authorities);
    }
}
