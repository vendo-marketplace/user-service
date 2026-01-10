package com.vendo.user_service.security.service;

import com.vendo.user_service.db.model.User;
import com.vendo.user_service.security.common.config.JwtProperties;
import com.vendo.user_service.security.common.helper.JwtHelper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.vendo.security.common.type.TokenClaim.*;
import static com.vendo.security.common.type.TokenClaim.STATUS_CLAIM;

@Service
@RequiredArgsConstructor
public class TestJwtService {

    private final JwtHelper jwtHelper;

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        List<String> authorities = jwtHelper.getAuthorities(user);

        return generateAccessToken(user, Map.of(
                USER_ID_CLAIM.getClaim(), user.getId(),
                EMAIL_VERIFIED_CLAIM.getClaim(), user.getEmailVerified(),
                ROLES_CLAIM.getClaim(), authorities,
                STATUS_CLAIM.getClaim(), user.getStatus()
        ));
    }

    private String generateAccessToken(User user, Map<String, Object> claims) {
        return buildToken(user, claims, jwtProperties.getAccessExpirationTime());
    }

    public String generateTokenWithExpiration(User user, int expiration) {
        List<String> authorities = jwtHelper.getAuthorities(user);
        return buildToken(user, Map.of(ROLES_CLAIM.getClaim(), authorities), expiration);
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
}
