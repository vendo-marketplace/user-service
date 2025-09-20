package com.vendo.user_service.security.token;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtils jwtUtils;

    private final JwtProperties jwtProperties;

    public static final String ROLES_CLAIM = "roles";

    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = jwtUtils.getRoles(userDetails);

        return generateAccessToken(userDetails, Map.of(ROLES_CLAIM, roles));
    }

    public String generateAccessToken(UserDetails userDetails, Map<String, Object> claims) {
        return buildToken(userDetails, claims, jwtProperties.getAccessExpirationTime());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        List<String> roles = jwtUtils.getRoles(userDetails);
        return generateRefreshToken(userDetails, Map.of(ROLES_CLAIM, roles));
    }

    public String generateRefreshToken(UserDetails userDetails, Map<String, Object> claims) {
        return buildToken(userDetails, claims, jwtProperties.getRefreshExpirationTime());
    }

    public String generateTokenWithExpiration(UserDetails userDetails, int expiration) {
        List<String> roles = jwtUtils.getRoles(userDetails);
        return buildToken(userDetails, Map.of(ROLES_CLAIM, roles), expiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String subject = jwtUtils.extractClaim(token, Claims::getSubject);
            return (!isTokenExpired(token) && userDetails.getUsername().equals(subject));
        } catch (JwtException exception) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return jwtUtils.extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (ExpiredJwtException exception) {
            return true;
        } catch (JwtException exception) {
            return false;
        }
    }

    private String buildToken(UserDetails userDetails, Map<String, Object> claims, int expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(jwtUtils.getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
