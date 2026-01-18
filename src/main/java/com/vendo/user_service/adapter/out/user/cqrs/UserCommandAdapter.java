package com.vendo.user_service.adapter.out.user.cqrs;

import com.vendo.user_service.adapter.out.user.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.adapter.out.user.model.User;
import com.vendo.user_service.adapter.out.user.repository.UserRepository;
import com.vendo.user_service.domain.user.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserCommandAdapter {

    private final UserRepository userRepository;

    public User save(User user) {
        userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
            throw new UserAlreadyExistsException("User already exists.");
        });

        return userRepository.save(user);
    }

    public void update(String userId, UserUpdateRequest requestUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        Optional.ofNullable(requestUser.password()).ifPresent(user::setPassword);
        Optional.ofNullable(requestUser.status()).ifPresent(user::setStatus);
        Optional.ofNullable(requestUser.providerType()).ifPresent(user::setProviderType);
        Optional.ofNullable(requestUser.fullName()).ifPresent(user::setFullName);
        Optional.ofNullable(requestUser.birthDate()).ifPresent(user::setBirthDate);
        Optional.ofNullable(requestUser.emailVerified()).ifPresent(user::setEmailVerified);

        userRepository.save(user);
    }
}
