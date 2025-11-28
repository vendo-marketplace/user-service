package com.vendo.user_service.model;

import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.common.util.WaitUtil;
import com.vendo.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
class UserAuditingTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSetCreatedAtAndUpdatedAt_whenUserSaved() {
        User user = UserDataBuilder.buildUserAllFields().build();
        Instant now = Instant.now();

        User saved = userRepository.save(user);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isCloseTo(now, within(300, ChronoUnit.MILLIS));
        assertThat(saved.getUpdatedAt()).isCloseTo(now, within(300, ChronoUnit.MILLIS));
    }

    @Test
    void shouldUpdateFieldUpdatedAt_whenUserModified() {
        User user = UserDataBuilder.buildUserAllFields().build();

        User saved = userRepository.save(user);
        Instant beforeUpdatedAt = saved.getUpdatedAt();

        WaitUtil.waitSafely(1);

        saved.setEmail("testupdate@gmail.com");
        User updated = userRepository.save(saved);
        Instant afterUpdatedAt = updated.getUpdatedAt();

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(updated.getCreatedAt());
        assertThat(beforeUpdatedAt).isBefore(afterUpdatedAt);
    }
}
