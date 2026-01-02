package com.vendo.user_service.service.user;

import com.vendo.user_service.db.model.User;

public interface UserProvisioningService {
    User ensureUserExists(String email);
}
