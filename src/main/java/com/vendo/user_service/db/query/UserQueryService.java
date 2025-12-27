package com.vendo.user_service.db.query;

import com.vendo.user_service.db.model.User;
import com.vendo.user_service.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserQueryService implements UserDetailsService {

    private final UserRepository userRepository;

    public Optional<User> findByEmail(String email) {
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
