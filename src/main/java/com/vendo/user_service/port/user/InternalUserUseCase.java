package com.vendo.user_service.port.user;

import com.vendo.user_service.adapter.user.in.dto.SaveUserRequest;
import com.vendo.user_service.adapter.user.in.dto.UpdateUserRequest;
import com.vendo.user_service.application.command.ExistsUserResponse;
import com.vendo.user_service.domain.user.User;

public interface InternalUserUseCase {

    User getById(String id);
    User getByEmail(String email);

    ExistsUserResponse existsByEmail(String email);

    void update(String id, UpdateUserRequest body);
    User save(SaveUserRequest body);

}
