package com.vendo.user_service.adapter.out.security.util;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

public class SecurityContextUtils {

    public static Authentication initializeAuthentication(String subject, Collection<? extends GrantedAuthority> authorities) {
        return new UsernamePasswordAuthenticationToken(
                subject,
                null,
                authorities);
    }

    public static SecurityContext initializeSecurityContext(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        return securityContext;
    }

}
