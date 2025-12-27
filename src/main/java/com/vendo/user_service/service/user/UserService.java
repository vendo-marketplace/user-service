package com.vendo.user_service.service.user;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.security.common.exception.UserBlockedException;
import com.vendo.security.common.exception.UserEmailNotVerifiedException;
import com.vendo.security.common.exception.UserIsUnactiveException;
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
        User userFromDb = userQueryService.loadUserByUsername(userFromContext.getEmail());
        return userMapper.toUserProfileResponse(userFromDb);
    }

    @Override
    @Transactional
    public User ensureUserExists(String email) {
        return userQueryService.findByEmail(email).orElseGet(() -> userCommandService.save(User.builder()
                .email(email)
                .role(UserAuthority.USER)
                .status(UserStatus.ACTIVE)
                .providerType(ProviderType.LOCAL)
                .build()));
    }

    @Override
    public void validateActivity(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new UserBlockedException("User is blocked.");
        }

        if (!user.isEmailVerified()) {
            throw new UserEmailNotVerifiedException("User email is not verified.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserIsUnactiveException("User is unactive.");
        }
    }
}
