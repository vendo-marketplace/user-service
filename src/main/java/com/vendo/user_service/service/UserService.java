package com.vendo.user_service.service;

import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.integration.redis.common.dto.UpdateUserRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void save(User user) {
        throwIfUserExistsByEmail(user.getEmail());
        userRepository.save(user);
    }

    public void update(String userId, UpdateUserRequest updateUserRequest) {
        User user = findByUserIdOrThrow(userId);

        Optional.ofNullable(updateUserRequest.password()).ifPresent(user::setPassword);

        userRepository.save(user);
    }

    public User findByUserIdOrThrow(@NotNull String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User findByEmailOrThrow(@NotNull String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public void throwIfUserExistsByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }
    }
}
