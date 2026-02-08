package com.vendo.user_service.adapter.out.user.persistence;

import com.vendo.user_service.adapter.out.user.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.adapter.out.user.common.mapper.UserMapper;
import com.vendo.user_service.domain.user.dto.SaveUserRequest;
import com.vendo.user_service.domain.user.dto.UpdateUserRequest;
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
    public MongoUser save(SaveUserRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new UserAlreadyExistsException("User already exists.");
        });

        return userRepository.save(userMapper.mapToUser(request));
    }

    @Override
    public void update(String id, UpdateUserRequest request) {
        userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        MongoUser mongoUser = userMapper.mapToUser(request);
        mongoUser.setId(id);

        userRepository.save(mongoUser);
    }
}
