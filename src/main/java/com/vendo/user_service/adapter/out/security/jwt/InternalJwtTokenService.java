package com.vendo.user_service.adapter.out.security.jwt;

import com.vendo.security_lib.exception.InvalidTokenException;
import com.vendo.security_lib.type.InternalTokenClaim;
import com.vendo.user_service.adapter.out.security.jwt.dto.InternalClaimPayload;
import com.vendo.user_service.adapter.out.security.jwt.props.JwtProperties;
import com.vendo.user_service.adapter.out.security.jwt.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalJwtTokenService implements TokenClaimsParser {

    private final JwtProperties jwtProperties;

    private final JwtUtils jwtUtils;

    @Override
    public InternalClaimPayload parseInternalClaims(String token) {
        try {
            Claims claims = jwtUtils.parseSignedClaims(token, jwtProperties.getKey()).getPayload();

            List<String> roles = parseRoles(claims.get(InternalTokenClaim.ROLES.getClaim()));
            String audience = claims.get(Claims.AUDIENCE, String.class);

            return new InternalClaimPayload(claims.getSubject(), roles, audience);
        } catch (JwtException e) {
            log.error(e.getMessage());
            throw new InvalidTokenException("Invalid token.");
        }
    }

    private List<String> parseRoles(Object payload) {
        if (payload instanceof List<?> list) {

            if (list.stream().allMatch(String.class::isInstance)) {
                return list.stream()
                        .map(String.class::cast)
                        .toList();
            }
        }

        throw new InvalidTokenException("Invalid roles.");
    }

}
