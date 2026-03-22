package com.vendo.user_service.adapter.in.user;

import com.vendo.user_service.adapter.out.user.mapper.UserMapper;
import com.vendo.user_service.application.InternalUserService;
import com.vendo.user_service.adapter.in.user.dto.SaveUserRequest;
import com.vendo.user_service.adapter.in.user.dto.UpdateUserRequest;
import com.vendo.user_service.application.command.ExistsUserResponse;
import com.vendo.user_service.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
@Slf4j
public class InternalUserController {

    private final InternalUserService internalUserService;

    private final UserMapper userMapper;

    @GetMapping
    ResponseEntity<User> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(internalUserService.getByEmail(email));
    }

    @GetMapping("/exists")
    ResponseEntity<ExistsUserResponse> existsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(internalUserService.existsByEmail(email));
    }

    @PutMapping
    void update(@RequestParam String id, @RequestBody UpdateUserRequest body) {
        internalUserService.update(id, userMapper.toUser(body));
    }

    @PostMapping
    ResponseEntity<User> save(@Valid @RequestBody SaveUserRequest body) {
        User user = userMapper.toUser(body);
        log.info("InternalUserContollerBodyMappedUser:{}",user);
        return ResponseEntity.ok(internalUserService.save(user));
    }
}