package com.vendo.user_service.adapter.in.security;

import com.vendo.security.common.exception.AccessDeniedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalGatewayFilter extends OncePerRequestFilter {

    private final InternalAntPathResolver internalAntPathResolver;

    // TODO move to common
    private String INTERNAL_HEADER = "X-Internal-Api-Key";

    @Value("${internal.api-key}")
    private String INTERNAL_API_KEY;

    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String apiKey = request.getHeader(INTERNAL_HEADER);
            validateApiKey(apiKey);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return !internalAntPathResolver.isPermittedPath(requestURI);
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || !apiKey.equals(INTERNAL_API_KEY)) {
            throw new AccessDeniedException("Invalid credentials.");
        }
    }
}
