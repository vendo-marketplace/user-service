package com.vendo.user_service;

import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.db.model.User;
import com.vendo.user_service.db.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_WithNull_ReturnsEmpty() {
        User user = UserDataBuilder.buildUserAllFields()
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        userRepository.save(user);

        Optional<User> result = userRepository.findByEmail(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByEmail_WithNull_FindsBrokenUser() {
        User user = UserDataBuilder.buildUserAllFields()
                .email(null)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        userRepository.save(user);

        Optional<User> result = userRepository.findByEmail(null);

        assertTrue(result.isPresent());
        assertEquals("Ghost", result.get().getFullName());
    }
}
