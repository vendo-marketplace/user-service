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

    private final UserMapper userMapper;

    private final UserRepository userRepository;

    @Override
    public User save(User user) {
        throwIfUserExists(user.getEmail());
        MongoUser saved = userRepository.save(userMapper.toMongoUser(user));
        return userMapper.toUser(saved);
    }

    @Override
    public void update(String id, User user) {
        throwIfUserNotExist(id);

        log.info("Before save: {}", user);

        MongoUser mongoUser = userMapper.toMongoUser(user);
        log.info("After mapping: {}", mongoUser);
        mongoUser.setId(id);

        MongoUser saved = userRepository.save(mongoUser);
        log.info("After save: {}", saved);
    }

    private void throwIfUserExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User already exists.");
        }
    }

    private void throwIfUserNotExist(String id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found.");
        }
    }
}