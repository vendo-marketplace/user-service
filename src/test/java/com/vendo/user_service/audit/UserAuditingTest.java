package com.vendo.user_service.audit;

import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserAuditingTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnCreated() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        User saved = userRepository.save(user);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void shouldUpdateFieldUpdatedAtOnModify() throws InterruptedException {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        User saved = userRepository.save(user);

        Instant createdAt = saved.getCreatedAt();
        Instant updatedAt = saved.getCreatedAt();

        Thread.sleep(10);

        saved.setEmail("testupdate@gmail.com");
        User updated = userRepository.save(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(updatedAt);

    }
}
