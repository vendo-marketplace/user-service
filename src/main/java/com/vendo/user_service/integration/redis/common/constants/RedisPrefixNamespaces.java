package com.vendo.user_service.integration.redis.common.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class RedisPrefixNamespaces {

    public static final String RESET_PASSWORD_EMAIL = "reset:email:";

    public static final String RESET_PASSWORD_TOKEN = "reset:token:";

}
