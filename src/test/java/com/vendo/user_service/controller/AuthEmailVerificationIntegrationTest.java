package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.service.RedisService;
import com.vendo.user_service.kafka.consumer.TestConsumer;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthEmailVerificationIntegrationTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TestConsumer testConsumer;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisProperties redisProperties;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
        userRepository.deleteAll();
    }

    @AfterTestClass
    void tearDown() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
        userRepository.deleteAll();
    }

    @Test
    void verifyEmail_shouldSendEmailVerificationEventSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/verification/send-code").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> otp = redisService.getValue(redisProperties.getEmailVerification().getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isPresent();
        assertThat(Integer.parseInt(otp.get())).isGreaterThan(99999).isLessThan(1000000);

        Optional<String> email = redisService.getValue(redisProperties.getEmailVerification().getOtp().buildPrefix(otp.get()));
        assertThat(email).isPresent();
        assertThat(email.get()).isEqualTo(user.getEmail());

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void verifyEmail_shouldReturnConflict_whenEmailVerificationEventHasAlreadySent() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        RedisProperties.EmailVerification emailVerification = redisProperties.getEmailVerification();
        redisService.saveValue(
                emailVerification.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(123456),
                emailVerification.getEmail().getTtl()
        );
        userRepository.save(user);

        String responseContent = mockMvc.perform(post("/auth/verification/send-code")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Otp has already sent to email.");

        Optional<String> otp = redisService.getValue(emailVerification.getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isPresent();

        await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(otp.get())).isFalse());
    }
}
