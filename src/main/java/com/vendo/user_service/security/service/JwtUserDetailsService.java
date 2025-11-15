package com.vendo.user_service.security.service;

import com.vendo.security.common.exception.InvalidTokenException;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.common.helper.JwtHelper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import static com.vendo.security.common.constants.AuthConstants.BEARER_PREFIX;

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

    public UserDetails retrieveUserDetails(String token) {
        Claims claims = jwtHelper.extractAllClaims(token);
        return userDetailsService.loadUserByUsername(claims.getSubject());
    }
}
