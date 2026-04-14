package com.vendo.user_service.adapter.user.out.persistence;

import com.vendo.user_lib.exception.UserAlreadyExistsException;
import com.vendo.user_lib.exception.UserNotFoundException;
import com.vendo.user_service.adapter.user.in.dto.SaveUserRequest;
import com.vendo.user_service.adapter.user.in.dto.UpdateUserRequest;
import com.vendo.user_service.adapter.user.out.mapper.UserMapper;
import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.port.user.UserCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCommandAdapter implements UserCommandPort {

    private final UserMapper userMapper;

    private final UserRepository userRepository;

    @Override
    public User save(SaveUserRequest body) {
        throwIfUserExists(body.email());
        MongoUser saved = userRepository.save(userMapper.toMongoUser(body));
        return userMapper.toUser(saved);
    }

    @Override
    public void update(String id, UpdateUserRequest body) {
        MongoUser user = getOrThrow(id);
        userMapper.updateUser(user, body);
        userRepository.save(user);
    }

    private void throwIfUserExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User already exists.");
        }
    }

    private MongoUser getOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
    }
}