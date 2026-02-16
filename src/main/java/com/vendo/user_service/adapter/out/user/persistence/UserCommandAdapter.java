package com.vendo.user_service.adapter.out.user.persistence;

import com.vendo.user_service.adapter.out.user.exception.UserAlreadyExistsException;
import com.vendo.user_service.adapter.out.user.mapper.UserMapper;
import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.port.user.UserCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCommandAdapter implements UserCommandPort {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    @Override
    public MongoUser save(User user) {
        userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
            throw new UserAlreadyExistsException("User already exists.");
        });

        return userRepository.save(userMapper.toMongoUser(user));
    }

    @Override
    public void update(String id, User user) {
        userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        MongoUser mongoUser = userMapper.toMongoUser(user);
        mongoUser.setId(id);

        userRepository.save(mongoUser);
    }
}
