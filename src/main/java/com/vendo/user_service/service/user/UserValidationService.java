package com.vendo.user_service.service.user;

import com.vendo.user_service.db.model.User;

public interface UserValidationService {
    void validate(User user);
}
