package com.vendo.user_service.port.user;

import com.vendo.user_service.domain.user.User;

public interface UserQueryPort {

    User getByEmail(String email);

    boolean existsByEmail(String email);

}
