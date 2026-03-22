package com.vendo.user_service.adapter.out.user.persistence;

import com.vendo.user_lib.exception.UserAlreadyExistsException;
import com.vendo.user_lib.exception.UserNotFoundException;
import com.vendo.user_service.adapter.out.user.mapper.UserMapper;
import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.port.user.UserCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCommandAdapter implements UserCommandPort {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
            throw new UserAlreadyExistsException("User already exists.");
        });
        log.info("Checkout user: {}", user);
        MongoUser mongoUser = userMapper.toMongoUser(user);
        log.info("Checkout Mongo user: {}", mongoUser);
        return userRepository.save(mongoUser);
    }

    @Override
    public void update(String id, User user) {
        userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        MongoUser mongoUser = userMapper.toMongoUser(user);
        mongoUser.setId(id);

        userRepository.save(mongoUser);
    }
}