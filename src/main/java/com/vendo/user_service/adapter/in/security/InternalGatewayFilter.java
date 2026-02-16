package com.vendo.user_service.adapter.in.security;

import com.vendo.common.type.Service;
import com.vendo.security.AntPathResolver;
import com.vendo.security.common.exception.InvalidTokenException;
import com.vendo.user_service.adapter.out.security.jwt.TokenValidationService;
import com.vendo.user_service.adapter.out.user.type.UserAuthority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

import static com.vendo.security.common.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.vendo.security.common.constants.AuthConstants.BEARER_PREFIX;

@Component
@RequiredArgsConstructor
public class InternalGatewayFilter extends OncePerRequestFilter {

    private final TokenValidationService tokenValidationService;

    private final AntPathResolver antPathResolver;

    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver handlerExceptionResolver;

    // TODO rewrite tests for filter
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext.getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = getTokenFromRequest(request.getHeader(AUTHORIZATION_HEADER));
            validateAuthorization(token);
            addAuthenticationToContext();
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
        if (authorization == null) {
            throw new AuthenticationCredentialsNotFoundException("Unauthorized.");
        } else if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new InvalidTokenException("Invalid token.");
        }

        return authorization.substring(BEARER_PREFIX.length());
    }

    private void validateAuthorization(String token) {
        boolean validate = tokenValidationService.validate(token);
        if (!validate) {
            throw new InvalidTokenException("Token is not valid.");
        }
    }

    private void addAuthenticationToContext() {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(Service.USER_SERVICE.getName(), List.of(new SimpleGrantedAuthority(UserAuthority.INTERNAL.getAuthority())));

        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
