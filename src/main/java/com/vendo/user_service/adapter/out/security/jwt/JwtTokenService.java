package com.vendo.user_service.adapter.out.security.jwt;

import com.vendo.user_service.adapter.out.security.jwt.props.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenService implements TokenValidationService {

    private final JwtProperties jwtProperties;

    @Override
    public boolean validate(String token) {
        try {
            Claims claims = extractAllClaims(token, jwtProperties.getKey());
            return claims != null;
        } catch (JwtException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public Claims extractAllClaims(String token, String secretKey) {
        return parseSignedClaims(token, secretKey).getPayload();
    }

    private Key getSignInKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private Jws<Claims> parseSignedClaims(String token, String secretKey) throws JwtException {
        return Jwts.parser()
                .verifyWith((SecretKey) getSignInKey(secretKey))
                .build()
                .parseSignedClaims(token);
    }
}
