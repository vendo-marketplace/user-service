package com.vendo.user_service.service.user;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.security.common.exception.UserBlockedException;
import com.vendo.user_service.common.exception.UserAlreadyActivatedException;
import com.vendo.user_service.common.mapper.UserMapper;
import com.vendo.user_service.db.command.UserCommandService;
import com.vendo.user_service.db.model.User;
import com.vendo.user_service.db.query.UserQueryService;
import com.vendo.user_service.security.common.type.UserAuthority;
import com.vendo.user_service.web.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vendo.user_service.security.common.helper.SecurityContextHelper.getUserFromContext;

@Service
@RequiredArgsConstructor
public class UserService implements UserProvisioningService, UserActivityValidationService {

    private final UserQueryService userQueryService;

    private final UserCommandService userCommandService;

    private final UserMapper userMapper;

    public UserProfileResponse getAuthenticatedUserProfile() {
        User userFromContext = getUserFromContext();
        return userMapper.toUserProfileResponse(userFromContext);
    }

    @Override
    @Transactional
    public User ensureExists(String email) {
        return userQueryService.findByEmail(email)
                .orElseGet(() -> userCommandService.save(User.builder()
                        .email(email)
                        .role(UserAuthority.USER)
                        .status(UserStatus.ACTIVE)
                        .providerType(ProviderType.LOCAL)
                        .build())
                );
    }

    @Override
    public void validateBeforeActivation(User user) {
        UserStatus status = user.getStatus();
        throwIfBlocked(status);
        throwIfActive(status);
    }

    private void throwIfBlocked(UserStatus userStatus) {
        if (userStatus == UserStatus.BLOCKED) {
            throw new UserBlockedException("User is blocked.");
        }
    }
    
    private void throwIfActive(UserStatus userStatus) {
        if (userStatus == UserStatus.ACTIVE) {
            throw new UserAlreadyActivatedException("Your account is already activated.");
        }
    }
}
