package com.vendo.user_service.adapter.out.security.jwt.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;

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
}
