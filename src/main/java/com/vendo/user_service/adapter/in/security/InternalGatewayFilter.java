package com.vendo.user_service.adapter.in.security;

import com.vendo.core_lib.type.ServiceName;
import com.vendo.core_lib.type.ServiceRole;
import com.vendo.security_lib.resolver.AntPathResolver;
import com.vendo.user_service.adapter.out.security.jwt.TokenClaimsParser;
import com.vendo.user_service.adapter.out.security.jwt.dto.InternalClaimPayload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

import static com.vendo.security_lib.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.vendo.security_lib.constants.AuthConstants.BEARER_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalGatewayFilter extends OncePerRequestFilter {

    private final TokenClaimsParser tokenClaimsParser;

    private final AntPathResolver antPathResolver;

    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext.getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = getTokenFromRequest(request.getHeader(AUTHORIZATION_HEADER));
            InternalClaimPayload claims = validateClaims(token);
            addAuthenticationToContext(claims);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return antPathResolver.isPermittedPath(requestURI);
    }

    private String getTokenFromRequest(String authorization) {
        if (authorization != null) {
            boolean startsWithBearer = authorization.startsWith(BEARER_PREFIX);
            boolean tokenIsNotEmpty = authorization.length() > BEARER_PREFIX.length() + 1;

            if (startsWithBearer && tokenIsNotEmpty) {
                return authorization.substring(BEARER_PREFIX.length());
            }
        }

        throw new AuthenticationCredentialsNotFoundException("Unauthorized.");
    }

    private InternalClaimPayload validateClaims(String token) {
        InternalClaimPayload claims = tokenClaimsParser.parseInternalClaims(token);

        boolean isUserService = ServiceName.valueOf(claims.audience()) == ServiceName.USER_SERVICE;
        boolean hasInternalRole = claims.roles().contains(ServiceRole.INTERNAL.toString());

        if (!isUserService || !hasInternalRole) {
            log.error("Invalid token claims {}.", claims);
            throw new BadCredentialsException("Invalid token.");
        }

        return claims;
    }

    private void addAuthenticationToContext(InternalClaimPayload claims) {
        List<SimpleGrantedAuthority> grantedAuthorities = claims.roles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(claims.subject(), null, grantedAuthorities);

        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
