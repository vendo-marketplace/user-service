package com.vendo.user_service.web.controller;

import com.vendo.user_service.service.auth.GoogleAuthService;
import com.vendo.user_service.web.dto.AuthResponse;
import com.vendo.user_service.web.dto.GoogleAuthRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@RequestMapping("/auth")
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping("/google")
    ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest googleAuthRequest) {
        return ResponseEntity.ok(googleAuthService.googleAuth(googleAuthRequest));
    }

}
