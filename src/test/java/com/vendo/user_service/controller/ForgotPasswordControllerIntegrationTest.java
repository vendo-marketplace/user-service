package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.user_service.builder.UserDataBuilder;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.common.dto.ForgotPasswordRequest;
import com.vendo.user_service.integration.redis.common.dto.ResetPasswordRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ForgotPasswordControllerIntegrationTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TestConsumer testConsumer;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisProperties redisProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void forgotPassword_shouldSentNotificationEventSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        ForgotPasswordRequest forgotPasswordRequest = ForgotPasswordRequest.builder().email(user.getEmail()).build();

        mockMvc.perform(post("/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andExpect(status().isOk());

        Optional<String> token = redisService.getValue(redisProperties.getResetPassword().getPrefixes().getEmailPrefix() + user.getEmail());
        assertThat(token).isPresent();
        assertThat(token.get()).isNotBlank();
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.hasReceived(token.get())).isTrue());
    }

    @Test
    void forgotPassword_shouldReturnConflict_whenMessageHasAlreadySent() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        ForgotPasswordRequest forgotPasswordRequest = ForgotPasswordRequest.builder().email(user.getEmail()).build();
        userRepository.save(user);
        redisService.saveValue(
                redisProperties.getResetPassword().getPrefixes().getEmailPrefix() + user.getEmail(),
                String.valueOf(UUID.randomUUID()),
                redisProperties.getResetPassword().getTtl()
        );

        String responseContent = mockMvc.perform(post("/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("Password recovery notification has already sent");

        Optional<String> target = redisService.getValue(redisProperties.getResetPassword().getPrefixes().getEmailPrefix() + user.getEmail());
        assertThat(target).isPresent();

        String token = target.get();
        assertThat(token).isNotBlank();
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.hasReceived(token)).isFalse());
    }

    @Test
    void forgotPassword_shouldReturnBadRequest_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        ForgotPasswordRequest forgotPasswordRequest = ForgotPasswordRequest.builder().email(user.getEmail()).build();

        String responseContent = mockMvc.perform(post("/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("User not found");

        Optional<String> target = redisService.getValue(redisProperties.getResetPassword().getPrefixes().getEmailPrefix() + user.getEmail());
        assertThat(target).isEmpty();
    }

    @Test
    void forgotPassword_shouldReturnBadRequest_whenRequestBodyIsInvalid() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        ForgotPasswordRequest forgotPasswordRequest = ForgotPasswordRequest.builder().build();

        String responseContent = mockMvc.perform(post("/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).contains("Email is required");

        Optional<String> target = redisService.getValue(redisProperties.getResetPassword().getPrefixes().getEmailPrefix() + user.getEmail());
        assertThat(target).isEmpty();
    }

    @Test
    void resetPassword_shouldResetPasswordSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String token = String.valueOf(UUID.randomUUID());
        String newPassword = "newTestPassword1234@";
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().password(newPassword).build();
        userRepository.save(user);
        redisService.saveValue(
                redisProperties.getResetPassword().getPrefixes().getTokenPrefix() + token,
                user.getEmail(),
                redisProperties.getResetPassword().getTtl()
        );

        mockMvc.perform(put("/password/reset").param("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();
        assertThat(passwordEncoder.matches(newPassword, optionalUser.get().getPassword())).isTrue();
        assertThat(redisService.hasActiveKey(redisProperties.getResetPassword().getPrefixes().getTokenPrefix() + token)).isFalse();
        assertThat(redisService.hasActiveKey(redisProperties.getResetPassword().getPrefixes().getEmailPrefix() + user.getEmail())).isFalse();
    }

    @Test
    void resetPassword_shouldReturnGone_whenTokenExpired() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String token = String.valueOf(UUID.randomUUID());
        String newPassword = "newTestPassword1234@";
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().password(newPassword).build();
        userRepository.save(user);
        redisService.saveValue(
                redisProperties.getResetPassword().getPrefixes().getTokenPrefix(),
                user.getEmail(),
                1
        );

        sleepSafely(1);
        String responseContent = mockMvc.perform(put("/password/reset").param("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isGone()).andReturn().getResponse().getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("Password recovery token has expired");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();
        assertThat(passwordEncoder.matches(newPassword, optionalUser.get().getPassword())).isFalse();
        assertThat(redisService.hasActiveKey(redisProperties.getResetPassword().getPrefixes().getTokenPrefix() + token)).isFalse();
        assertThat(redisService.hasActiveKey(redisProperties.getResetPassword().getPrefixes().getEmailPrefix() + user.getEmail())).isFalse();
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String token = String.valueOf(UUID.randomUUID());
        String newPassword = "newTestPassword1234@";
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().password(newPassword).build();
        redisService.saveValue(
                redisProperties.getResetPassword().getPrefixes().getTokenPrefix() + token,
                user.getEmail(),
                redisProperties.getResetPassword().getTtl()
        );

        String responseContent = mockMvc.perform(put("/password/reset").param("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("User not found");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isNotPresent();
    }

    private void sleepSafely(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
