package com.vendo.user_service.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtUserDetailsService {

    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

    public TokenPayload generateTokenPayload(UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("User details not present");
        }

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return TokenPayload.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public UserDetails getUserDetailsIfTokenValidOrThrow(String token) {
        String subject = jwtService.extractSubject(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(subject);
        boolean isTokenValid = jwtService.isTokenValid(token, userDetails);

        if (isTokenValid) {
            return userDetails;
        }

        throw new BadCredentialsException("Token not valid!");
    }
}
