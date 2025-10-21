package com.vendo.user_service.web.controller;

import com.vendo.user_service.service.AuthService;
import com.vendo.user_service.service.EmailVerificationService;
import com.vendo.user_service.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/sign-in")
    ResponseEntity<AuthResponse> signIn(@Valid @RequestBody AuthRequest authRequest) {
        return ResponseEntity.ok(authService.signIn(authRequest));
    }

    @PostMapping("/sign-up")
    void signUp(@Valid @RequestBody AuthRequest authRequest) {
        authService.signUp(authRequest);
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
        return ResponseEntity.ok(authService.refresh(refreshRequest));
    }

    @PostMapping("/verification/send-code")
    void sendVerificationCode(@RequestParam String email) {
        emailVerificationService.sendVerificationCode(email);
    }

    @PostMapping("/verification/resend-code")
    void resendVerificationCode(@RequestParam String email) {
        emailVerificationService.resendVerificationCode(email);
    }
}
