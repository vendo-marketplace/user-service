package com.vendo.user_service.security.service;

import com.vendo.user_service.db.model.User;
import com.vendo.user_service.security.common.config.JwtProperties;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.common.helper.JwtHelper;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.vendo.security.common.type.TokenClaim.*;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtHelper jwtHelper;

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        List<String> roles = jwtHelper.getRoles(user);

        return generateAccessToken(user, Map.of(
                USER_ID_CLAIM.getClaim(), user.getId(),
                EMAIL_VERIFIED_CLAIM.getClaim(), user.isEmailVerified(),
                ROLES_CLAIM.getClaim(), roles,
                STATUS_CLAIM.getClaim(), user.getStatus()
        ));
    }

    public String generateAccessToken(User user, Map<String, Object> claims) {
        return buildToken(user, claims, jwtProperties.getAccessExpirationTime());
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, Map.of(), jwtProperties.getRefreshExpirationTime());
    }

    public String generateTokenWithExpiration(User user, int expiration) {
        List<String> roles = jwtHelper.getRoles(user);
        return buildToken(user, Map.of(ROLES_CLAIM.getClaim(), roles), expiration);
    }

    private String buildToken(User user, Map<String, Object> claims, int expiration) {
        if (user == null) {
            throw new IllegalArgumentException("User is required.");
        }

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(jwtHelper.getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public TokenPayload generateTokenPayload(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required.");
        }

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        return TokenPayload.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
