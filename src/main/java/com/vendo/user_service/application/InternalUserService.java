package com.vendo.user_service.application;

import com.vendo.user_service.adapter.user.in.dto.SaveUserRequest;
import com.vendo.user_service.adapter.user.in.dto.UpdateUserRequest;
import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.application.command.ExistsUserResponse;
import com.vendo.user_service.port.user.InternalUserUseCase;
import com.vendo.user_service.port.user.UserCommandPort;
import com.vendo.user_service.port.user.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalUserService implements InternalUserUseCase {

    private final UserCommandPort userCommandPort;

    private final UserQueryPort userQueryPort;

    @Override
    public User getById(String id) {
        return userQueryPort.getById(id);
    }

    @Override
    public User getByEmail(String email) {
        return userQueryPort.getByEmail(email);
    }

    @Override
    public ExistsUserResponse existsByEmail(String email) {
        return ExistsUserResponse.builder().exists(userQueryPort.existsByEmail(email)).build();
    }

    @Override
    public void update(String id, UpdateUserRequest body) {
        userCommandPort.update(id, body);
    }

    @Override
    public User save(SaveUserRequest body) {
        return userCommandPort.save(body);
    }
}
