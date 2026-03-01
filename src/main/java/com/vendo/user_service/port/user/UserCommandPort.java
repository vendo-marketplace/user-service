package com.vendo.user_service.port.user;

import com.vendo.user_service.domain.user.User;

public interface UserCommandPort {

    User save(User user);

    void update(String userId, User user);

}
