package com.vendo.user_service.port.user;

import com.vendo.user_service.adapter.user.in.dto.SaveUserRequest;
import com.vendo.user_service.adapter.user.in.dto.UpdateUserRequest;
import com.vendo.user_service.domain.user.User;

public interface UserCommandPort {

    User save(SaveUserRequest body);

    void update(String userId, UpdateUserRequest body);

}