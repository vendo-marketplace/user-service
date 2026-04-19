package com.vendo.user_service.adapter.user.out.persistence;

import com.vendo.user_lib.exception.UserNotFoundException;
import com.vendo.user_service.adapter.user.out.mapper.UserMapper;
import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.port.user.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserQueryAdapter implements UserQueryPort {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public User getByEmail(String email) {
        MongoUser mongoUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        return userMapper.toUser(mongoUser);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}