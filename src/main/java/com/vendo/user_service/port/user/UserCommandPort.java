package com.vendo.user_service.port.user;

import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.domain.user.dto.SaveUserRequest;
import com.vendo.user_service.domain.user.dto.UpdateUserRequest;

public interface UserCommandPort {

    User save(SaveUserRequest request);

    void update(String userId, UpdateUserRequest requestUser);

}
