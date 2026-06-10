package com.vendo.user_service.adapter.user.in;

import com.vendo.user_service.adapter.user.in.dto.SaveUserRequest;
import com.vendo.user_service.adapter.user.in.dto.UpdateUserRequest;
import com.vendo.user_service.application.command.ExistsUserResponse;
import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.port.user.InternalUserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {

    private final InternalUserUseCase useCase;

    @GetMapping
    ResponseEntity<User> getById(@RequestParam String id) {
        return ResponseEntity.ok(useCase.getById(id));
    }

    @GetMapping
    ResponseEntity<User> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(useCase.getByEmail(email));
    }

    @GetMapping("/exists")
    ResponseEntity<ExistsUserResponse> existsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(useCase.existsByEmail(email));
    }

    @PutMapping
    void update(@RequestParam String id, @RequestBody UpdateUserRequest body) {
        useCase.update(id, body);
    }

    @PostMapping
    ResponseEntity<User> save(@Valid @RequestBody SaveUserRequest body) {
        return ResponseEntity.ok(useCase.save(body));
    }
}