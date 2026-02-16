package com.vendo.user_service.adapter.out.security.jwt;

public interface TokenValidationService {

    boolean validate(String token);

}
