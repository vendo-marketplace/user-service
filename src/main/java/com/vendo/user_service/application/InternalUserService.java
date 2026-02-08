package com.vendo.user_service.application;

import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.domain.user.dto.ExistsUserResponse;
import com.vendo.user_service.domain.user.dto.SaveUserRequest;
import com.vendo.user_service.domain.user.dto.UpdateUserRequest;
import com.vendo.user_service.port.user.UserCommandPort;
import com.vendo.user_service.port.user.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalUserService {

    private final UserCommandPort userCommandPort;

    private final UserQueryPort userQueryPort;

    public User getByEmail(String email) {
        return userQueryPort.getByEmail(email);
    }

    public ExistsUserResponse existsByEmail(String email) {
        return ExistsUserResponse.builder().exists(userQueryPort.existsByEmail(email)).build();
    }

    public void update(String id, UpdateUserRequest updateUserRequest) {
        userCommandPort.update(id, updateUserRequest);
    }

    public User save(SaveUserRequest saveUserRequest) {
        return userCommandPort.save(saveUserRequest);
    }
}
