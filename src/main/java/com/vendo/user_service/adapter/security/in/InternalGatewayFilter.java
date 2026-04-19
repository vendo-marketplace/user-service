package com.vendo.user_service.adapter.security.in;

import com.vendo.core_lib.type.ServiceName;
import com.vendo.core_lib.type.ServiceRole;
import com.vendo.security_lib.resolver.AntPathResolver;
import com.vendo.user_service.adapter.security.out.TokenClaimsParser;
import com.vendo.user_service.adapter.security.out.dto.InternalClaimPayload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AuthenticationServiceException("Unauthorized.");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.info("Request: {}.", requestURI);
        log.debug("Request: {}.", requestURI);
        var b = antPathResolver.isPermittedPath(requestURI);
        log.info("Permitted: {}.", b);
        return b;
    }

    private String getTokenFromRequest(String authorization) {
        if (authorization == null) {
            throw new AuthenticationCredentialsNotFoundException("Unauthorized.");
        } else if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("Invalid token.");
        }

        return authorization.substring(BEARER_PREFIX.length());
    }

    private InternalClaimPayload validateClaims(String token) {
        InternalClaimPayload claims = tokenClaimsParser.parseInternalClaims(token);

        boolean isUserService = claims.audience().contains(ServiceName.USER_SERVICE.toString());
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
