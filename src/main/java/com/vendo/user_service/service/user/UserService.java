package com.vendo.user_service.service.user;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.mapper.UserMapper;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.web.dto.UserProfileResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserProfileResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException("User is not authenticated.");
        }

        return userMapper.toUserProfileResponse((User) authentication.getPrincipal());
    }

    public User save(User user) {
        findByEmail(user.getEmail()).ifPresent(userResponse -> {
            throw new UserAlreadyExistsException("User already exists.");
        });
        return userRepository.save(user);
    }

    public void update(String userId, User requestUser) {
        User user = findByUserIdOrThrow(userId);

        Optional.ofNullable(requestUser.getPassword()).ifPresent(user::setPassword);
        Optional.ofNullable(requestUser.getStatus()).ifPresent(user::setStatus);
        Optional.ofNullable(requestUser.getProviderType()).ifPresent(user::setProviderType);

        userRepository.save(user);
    }

    public User findUserByEmailOrSave(String email) {
        return findByEmail(email).orElseGet(() -> save(User.builder()
                .email(email)
                .role(UserRole.USER)
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

    public User findByEmailOrThrow(@NotNull String email) {
        return findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}
