package com.vendo.user_service.security.service;

public interface BearerTokenExtractor {

    String parseBearerToken(String bearerToken);

}
