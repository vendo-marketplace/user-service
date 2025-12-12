package com.vendo.user_service.service.user;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.common.mapper.UserMapper;
import com.vendo.user_service.security.common.type.UserAuthorities;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.web.dto.UserProfileResponse;
import com.vendo.user_service.web.dto.UserUpdateRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.vendo.user_service.security.common.helper.SecurityContextHelper.getUserFromContext;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    public UserProfileResponse getAuthenticatedUserProfile() {
        User userFromContext = getUserFromContext();
        User userFromDb = loadUserByUsername(userFromContext.getEmail());
        return userMapper.toUserProfileResponse(userFromDb);
    }

    public User save(User user) {
        findByEmail(user.getEmail()).ifPresent(userResponse -> {
            throw new UserAlreadyExistsException("User already exists.");
        });
        return userRepository.save(user);
    }

    public void update(String userId, UserUpdateRequest requestUser) {
        User user = findByUserIdOrThrow(userId);

        Optional.ofNullable(requestUser.password()).ifPresent(user::setPassword);
        Optional.ofNullable(requestUser.status()).ifPresent(user::setStatus);
        Optional.ofNullable(requestUser.providerType()).ifPresent(user::setProviderType);
        Optional.ofNullable(requestUser.fullName()).ifPresent(user::setFullName);
        Optional.ofNullable(requestUser.birthDate()).ifPresent(user::setBirthDate);
        Optional.ofNullable(requestUser.emailVerified()).ifPresent(user::setEmailVerified);

        userRepository.save(user);
    }

    public User findUserByEmailOrSave(String email) {
        return findByEmail(email).orElseGet(() -> save(User.builder()
                .email(email)
                .role(UserAuthorities.USER)
                .status(UserStatus.ACTIVE)
                .providerType(ProviderType.LOCAL)
                .build()));
    }

    public User findByUserIdOrThrow(@NotNull String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }

    public Optional<User> findByEmail(@NotNull String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null) {
            throw new UsernameNotFoundException("User not found.");
        }

        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}
