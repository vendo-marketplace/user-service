package com.vendo.user_service.security.service;

import com.vendo.user_service.common.type.UserStatus;
import com.vendo.user_service.model.User;
import com.vendo.user_service.security.common.config.JwtProperties;
import com.vendo.user_service.security.common.helper.JwtHelper;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.vendo.security.common.type.TokenClaim.ROLES_CLAIM;
import static com.vendo.security.common.type.TokenClaim.STATUS_CLAIM;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtHelper jwtHelper;

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = jwtHelper.getRoles(userDetails);
        UserStatus status = ((User) userDetails).getStatus();

        return generateAccessToken(userDetails, Map.of(
                ROLES_CLAIM.getClaim(), roles,
                STATUS_CLAIM.getClaim(), status
        ));
    }

    public String generateAccessToken(UserDetails userDetails, Map<String, Object> claims) {
        return buildToken(userDetails, claims, jwtProperties.getAccessExpirationTime());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, Map.of(), jwtProperties.getRefreshExpirationTime());
    }

    public String generateTokenWithExpiration(UserDetails userDetails, int expiration) {
        List<String> roles = jwtHelper.getRoles(userDetails);
        return buildToken(userDetails, Map.of(ROLES_CLAIM.getClaim(), roles), expiration);
    }

    private String buildToken(UserDetails userDetails, Map<String, Object> claims, int expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(jwtHelper.getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
