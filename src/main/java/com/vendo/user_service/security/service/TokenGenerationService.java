package com.vendo.user_service.security.service;

import com.vendo.user_service.db.model.User;
import com.vendo.user_service.security.common.dto.TokenPayload;

public interface TokenGenerationService {

    TokenPayload generateTokensPair(User user);

}
