package com.vendo.user_service.adapter.security.in;

import com.vendo.core_lib.type.ServiceName;
import com.vendo.core_lib.type.ServiceRole;
import com.vendo.core_lib.utils.StringUtils;
import com.vendo.security_lib.http.HttpUtils;
import com.vendo.security_lib.resolver.AntPathResolver;
import com.vendo.security_starter.filter.utils.FilterUtils;
import com.vendo.security_starter.jwt.parser.TokenClaims;
import com.vendo.security_starter.jwt.parser.TokenClaimsParser;
import com.vendo.user_service.adapter.security.out.props.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalFilter extends OncePerRequestFilter {

    private final JwtProperties props;

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
            String token = HttpUtils.getTokenFrom(request.getHeader(HttpUtils.AUTHORIZATION_HEADER));
            TokenClaims claims = validateClaims(token);
            FilterUtils.addAuthToContext(claims, claims.roles());
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
        return antPathResolver.isPermittedPath(requestURI);
    }

    private TokenClaims validateClaims(String token) {
        TokenClaims claims = tokenClaimsParser.extract(token, props.getInternal().key());

        if (StringUtils.isEmpty(claims.subject()) || !claims.subject().equals(ServiceName.AUTH_SERVICE.name())) {
            throw new BadCredentialsException("Invalid subject %s.".formatted(claims.subject()));
        }

        if (CollectionUtils.isEmpty(claims.roles()) || !claims.roles().contains(ServiceRole.INTERNAL.name())) {
            throw new BadCredentialsException("Invalid roles %s.".formatted(claims.roles()));
        }

        if (CollectionUtils.isEmpty(claims.audience()) || !claims.audience().contains(ServiceName.USER_SERVICE.name())) {
            throw new BadCredentialsException("Invalid audience %s.".formatted(claims.audience()));
        }

        return claims;
    }
}
