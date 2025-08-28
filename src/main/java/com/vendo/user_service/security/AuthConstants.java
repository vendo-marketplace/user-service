package com.vendo.user_service.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthConstants {

    static final String AUTHORIZATION_HEADER = "Authorization";

    static final String BEARER_PREFIX = "Bearer ";

}
