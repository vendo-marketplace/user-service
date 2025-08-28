package com.vendo.user_service.web.controller;

import com.vendo.user_service.service.AuthService;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    void signUp(@RequestBody AuthRequest authRequest) {
        authService.signUp(authRequest);
    }

    @PostMapping("/sign-in")
    ResponseEntity<AuthResponse> signIn(@RequestBody AuthRequest authRequest) {
        return ResponseEntity.ok(authService.signIn(authRequest));
    }

    @PostMapping("/refresh-token")
    ResponseEntity<AuthResponse> refresh() {
        return ResponseEntity.ok(authService.refresh());
    }

}
