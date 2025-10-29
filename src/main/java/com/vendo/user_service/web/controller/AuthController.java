package com.vendo.user_service.web.controller;

import com.vendo.user_service.system.redis.common.dto.ValidateRequest;
import com.vendo.user_service.service.user.AuthService;
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

    @PostMapping("/send-otp")
    void sendVerificationCode(@RequestParam String email) {
        authService.sendOtp(email);
    }

    @PostMapping("/resend-otp")
    void resendOtp(@RequestParam String email) {
        authService.resendOtp(email);
    }

    @PostMapping("/validate")
    void validate(@Valid @RequestBody ValidateRequest validateRequest) {
        authService.validate(validateRequest);
    }
}
