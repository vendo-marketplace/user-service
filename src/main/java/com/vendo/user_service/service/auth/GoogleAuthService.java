package com.vendo.user_service.service.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.db.command.UserCommandService;
import com.vendo.user_service.db.model.User;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.service.user.UserProvisioningService;
import com.vendo.user_service.web.dto.AuthResponse;
import com.vendo.user_service.web.dto.GoogleAuthRequest;
import com.vendo.user_service.web.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final JwtService jwtService;

    private final UserCommandService userCommandService;

    private final UserProvisioningService userProvisioningService;

    private final GoogleOAuthService googleOauthService;

    public AuthResponse googleAuth(GoogleAuthRequest googleAuthRequest) {
        GoogleIdToken.Payload payload = googleOauthService.verify(googleAuthRequest.idToken());

        User user = userProvisioningService.ensureExists(payload.getEmail());

        if (user.getStatus() == UserStatus.INCOMPLETE) {
            userCommandService.update(user.getId(), UserUpdateRequest.builder()
                    .status(UserStatus.ACTIVE)
                    .providerType(ProviderType.GOOGLE).build()
            );
        }

        TokenPayload tokenPayload = jwtService.generateTokenPayload(user);
        return AuthResponse.builder()
                .accessToken(tokenPayload.accessToken())
                .refreshToken(tokenPayload.refreshToken())
                .build();
    }
}
