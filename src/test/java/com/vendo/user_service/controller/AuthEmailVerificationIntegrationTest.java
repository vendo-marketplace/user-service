package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.service.RedisService;
import com.vendo.user_service.kafka.consumer.TestConsumer;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import org.assertj.core.data.Percentage;
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

    @Test
    void verifyEmail_shouldReturnBadRequest_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        String responseContent = mockMvc.perform(post("/auth/verification/send-code")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("User not found.");

        Optional<String> otp = redisService.getValue(redisProperties.getEmailVerification().getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isEmpty();
    }

    @Test
    void resendOtp_shouldResendOtp_whenFirstAttemptAndOtpAlreadyExist() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.EmailVerification emailVerification = redisProperties.getEmailVerification();
        redisService.saveValue(
                emailVerification.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                emailVerification.getEmail().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(post("/auth/verification/resend-code").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(emailVerification.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(1);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldResendOtp_whenFirstAttemptAndOtpDoesNotExist() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.EmailVerification emailVerification = redisProperties.getEmailVerification();
        redisService.saveValue(
                emailVerification.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                emailVerification.getEmail().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(post("/auth/verification/resend-code").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(emailVerification.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(1);

        Long attemptsExpire = redisService.getExpire(emailVerification.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attemptsExpire).isNotNull();
        assertThat(attemptsExpire).isCloseTo(emailVerification.getAttempts().getTtl(), Percentage.withPercentage(5));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldBadRequest_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.EmailVerification emailVerification = redisProperties.getEmailVerification();
        redisService.saveValue(
                emailVerification.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                emailVerification.getEmail().getTtl()
        );

        String responseContent = mockMvc.perform(post("/auth/verification/resend-code").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("User not found.");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isNotPresent();

        await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isFalse());
        Optional<String> attempts = redisService.getValue(emailVerification.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isEmpty();
    }

    @Test
    void resendOtp_shouldResendOtp_whenSecondAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.EmailVerification emailVerification = redisProperties.getEmailVerification();
        redisService.saveValue(
                emailVerification.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                emailVerification.getEmail().getTtl()
        );
        redisService.saveValue(
                emailVerification.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(1),
                emailVerification.getAttempts().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(post("/auth/verification/resend-code").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(emailVerification.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(2);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldResendOtp_whenThirdAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.EmailVerification emailVerification = redisProperties.getEmailVerification();
        redisService.saveValue(
                emailVerification.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                emailVerification.getEmail().getTtl()
        );
        redisService.saveValue(
                emailVerification.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(2),
                emailVerification.getAttempts().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(post("/auth/verification/resend-code").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(emailVerification.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(3);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldReturnTooManyRequests_whenFourthAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.EmailVerification emailVerification = redisProperties.getEmailVerification();
        redisService.saveValue(
                emailVerification.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                emailVerification.getEmail().getTtl()
        );
        redisService.saveValue(
                emailVerification.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(3),
                emailVerification.getAttempts().getTtl()
        );
        userRepository.save(user);

        String responseContent = mockMvc.perform(post("/auth/verification/resend-code").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Reached maximum attempts for resending otp code.");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(emailVerification.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(3);

        await().atMost(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isFalse());
    }

    @Test
    void resendOtp_shouldReturnGone_whenResetSessionExpired() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        RedisProperties.EmailVerification emailVerification = redisProperties.getEmailVerification();
        userRepository.save(user);

        String responseContent = mockMvc.perform(post("/auth/verification/resend-code").param("email", user.getEmail())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Verification session expired.");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> otp = redisService.getValue(emailVerification.getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isNotPresent();
    }
}
