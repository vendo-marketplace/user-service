package com.vendo.user_service.security.service;

public interface JwtClaimsParser {

    String parseEmailFromToken(String jwtToken);

}
