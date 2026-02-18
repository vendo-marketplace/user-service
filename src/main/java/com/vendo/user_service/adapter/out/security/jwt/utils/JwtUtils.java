package com.vendo.user_service.adapter.out.security.jwt.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public final class JwtUtils {

    public Key getSignInKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public Jws<Claims> parseSignedClaims(String token, String secretKey) throws JwtException {
        return Jwts.parser()
                .verifyWith((SecretKey) getSignInKey(secretKey))
                .build()
                .parseSignedClaims(token);
    }

    public String buildToken(String secretKey, @Valid JwtPayload jwtPayload) {
        return Jwts.builder()
                .subject(jwtPayload.subject())
                .claims(jwtPayload.claims())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtPayload.expiration()))
                .signWith(getSignInKey(secretKey), SignatureAlgorithm.HS256)
                .compact();
    }

    public record JwtPayload(
            @NotBlank(message = "Subject is required.")
            String subject,

            Map<String, Object> claims,
            int expiration) {
    }

}
