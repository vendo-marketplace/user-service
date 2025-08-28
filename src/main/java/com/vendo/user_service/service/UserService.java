package com.vendo.user_service.service;

import com.vendo.user_service.exception.UserAlreadyExistsException;
import com.vendo.user_service.exception.UserNotFoundException;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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

    public User findByEmailOrThrow(@NotNull String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public void throwIfUserExistsByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }
    }

}
