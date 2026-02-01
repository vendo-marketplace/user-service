package com.vendo.user_service.adapter.out.user.persistence;

import com.vendo.user_service.adapter.out.user.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.domain.user.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserCommandAdapter {

    private final UserRepository userRepository;

    public MongoUser save(MongoUser mongoUser) {
        userRepository.findByEmail(mongoUser.getEmail()).ifPresent(u -> {
            throw new UserAlreadyExistsException("User already exists.");
        });

        return userRepository.save(mongoUser);
    }

    public void update(String userId, UserUpdateRequest requestUser) {
        MongoUser mongoUser = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        Optional.ofNullable(requestUser.password()).ifPresent(mongoUser::setPassword);
        Optional.ofNullable(requestUser.status()).ifPresent(mongoUser::setStatus);
        Optional.ofNullable(requestUser.providerType()).ifPresent(mongoUser::setProviderType);
        Optional.ofNullable(requestUser.fullName()).ifPresent(mongoUser::setFullName);
        Optional.ofNullable(requestUser.birthDate()).ifPresent(mongoUser::setBirthDate);
        Optional.ofNullable(requestUser.emailVerified()).ifPresent(mongoUser::setEmailVerified);

        userRepository.save(mongoUser);
    }
}
