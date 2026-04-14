package com.vendo.user_service.adapter.security.out;

import com.vendo.security_lib.type.InternalTokenClaim;
import com.vendo.user_service.adapter.security.out.dto.InternalClaimPayload;
import com.vendo.user_service.adapter.security.out.props.InternalJwtProperties;
import com.vendo.user_service.adapter.security.out.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalJwtTokenService implements TokenClaimsParser {

    private final InternalJwtProperties internalJwtProperties;

    private final JwtUtils jwtUtils;

    @Override
    public InternalClaimPayload parseInternalClaims(String token) {
        try {
            Claims claims = jwtUtils.parseSignedClaims(token, internalJwtProperties.getKey()).getPayload();

            List<String> roles = parseRoles(claims.get(InternalTokenClaim.ROLES.getClaim()));
            Set<String> audience = claims.getAudience();

            return new InternalClaimPayload(claims.getSubject(), roles, audience);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new BadCredentialsException(e.getMessage());
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

        throw new BadCredentialsException("Invalid roles.");
    }

}
