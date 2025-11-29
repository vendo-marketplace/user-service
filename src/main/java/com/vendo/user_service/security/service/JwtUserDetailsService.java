package com.vendo.user_service.security.service;

import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.model.User;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.common.helper.JwtHelper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtUserDetailsService {

    private final JwtHelper jwtHelper;

    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

    public TokenPayload generateTokenPayload(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User details is required.");
        }

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return TokenPayload.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public UserDetails getUserDetailsFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new AuthenticationCredentialsNotFoundException("Unauthorized.");
        }

        return userDetailsService.loadUserByUsername(userDetails.getUsername());
    }

    public UserDetails getUserDetailsByTokenSubject(String token) {
        Claims claims = jwtHelper.extractAllClaims(token);
        return userDetailsService.loadUserByUsername(claims.getSubject());
    }

    public static boolean isUserActive(UserDetails userDetails) {
        return userDetails instanceof User && ((User) userDetails).getStatus() == UserStatus.ACTIVE;
    }
}
