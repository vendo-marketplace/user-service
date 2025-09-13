package com.vendo.user_service.security;

import com.vendo.user_service.model.User;
import com.vendo.user_service.common.type.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.vendo.user_service.common.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.vendo.user_service.common.constants.AuthConstants.BEARER_PREFIX;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

//    private final UserAntPathResolver userAntPathResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext.getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = getTokenFromRequest(request);
        UserDetails userDetails = validateUserAccessibility(jwtToken);
        addAuthenticationToContext(userDetails);
    }

//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String servletPath = request.getServletPath();
//        return userAntPathResolver.isPermittedPath(servletPath);
//    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }

        throw new AccessDeniedException("Authorization failed");
    }

    private UserDetails validateUserAccessibility(String jwtToken) {
        String email = jwtService.extractSubject(jwtToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (userDetails instanceof User && ((User) userDetails).getStatus() == UserStatus.BLOCKED) {
            throw new AccessDeniedException("User is blocked");
        }

        return userDetails;
    }

    private void addAuthenticationToContext(UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
