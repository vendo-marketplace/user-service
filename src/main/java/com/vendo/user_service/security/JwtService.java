package com.vendo.user_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    public static final String ROLES_CLAIM = "roles";

    @Value("${security.jwt.secret-key}")
    private String jwtSecretKey;

    @Value("${security.jwt.access-expiration-time}")
    public int JWT_ACCESS_EXPIRATION_TIME;

    @Value("${security.jwt.refresh-expiration-time}")
    public int JWT_REFRESH_EXPIRATION_TIME;

    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = getRoles(userDetails);
        return generateAccessToken(userDetails, Map.of(ROLES_CLAIM, roles));
    }

    public String generateAccessToken(UserDetails userDetails, Map<String, Object> claims) {
        return buildToken(userDetails, claims, JWT_ACCESS_EXPIRATION_TIME);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        List<String> roles = getRoles(userDetails);
        return generateRefreshToken(userDetails, Map.of(ROLES_CLAIM, roles));
    }

    public String generateRefreshToken(UserDetails userDetails, Map<String, Object> claims) {
        return buildToken(userDetails, claims, JWT_REFRESH_EXPIRATION_TIME);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String subject = extractClaim(token, Claims::getSubject);
            return (!isTokenExpired(token) && userDetails.getUsername().equals(subject));
        } catch (JwtException exception) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> getRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
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
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
