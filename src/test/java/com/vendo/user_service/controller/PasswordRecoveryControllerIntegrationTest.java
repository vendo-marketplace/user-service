package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.common.dto.ResetPasswordRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.vendo.user_service.common.helper.WaitHelper.waitSafely;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PasswordRecoveryControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

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

// TODO update otp generation logic
//    Expecting actual:
//            27889
//    to be greater than:
//            99999
    @Test
    void forgotPassword_shouldSendForgotPasswordEventSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);

        mockMvc.perform(post("/password/forgot").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> otp = redisService.getValue(redisProperties.getPasswordRecovery().getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isPresent();
        assertThat(Integer.parseInt(otp.get())).isGreaterThan(99999).isLessThan(1000000);

        Optional<String> email = redisService.getValue(redisProperties.getPasswordRecovery().getOtp().buildPrefix(otp.get()));
        assertThat(email).isPresent();
        assertThat(email.get()).isEqualTo(user.getEmail());

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void forgotPassword_shouldReturnConflict_whenForgotPasswordEventHasAlreadySent() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        redisService.saveValue(
                recoveryProperties.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(123456),
                recoveryProperties.getEmail().getTtl()
        );
        userRepository.save(user);

        String responseContent = mockMvc.perform(post("/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("Otp has already sent to email.");

        Optional<String> otp = redisService.getValue(recoveryProperties.getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isPresent();

        assertThat(otp.get()).isNotBlank();
        await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(otp.get())).isFalse());
    }

    @Test
    void forgotPassword_shouldReturnBadRequest_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        String responseContent = mockMvc.perform(post("/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("User not found.");

        Optional<String> otp = redisService.getValue(redisProperties.getPasswordRecovery().getEmail().buildPrefix((user.getEmail())));
        assertThat(otp).isEmpty();
    }

    @Test
    void resetPassword_shouldResetPassword() throws Exception {
        Integer otp = 123456;
        String newPassword = "newTestPassword1234@";
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder()
                .otp(otp)
                .password(newPassword).build();
        redisService.saveValue(
                recoveryProperties.getOtp().buildPrefix(String.valueOf(otp)),
                user.getEmail(),
                recoveryProperties.getOtp().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(put("/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();
        assertThat(passwordEncoder.matches(newPassword, optionalUser.get().getPassword())).isTrue();
        assertThat(redisService.hasActiveKey(recoveryProperties.getOtp().buildPrefix(String.valueOf(otp)))).isFalse();
        assertThat(redisService.hasActiveKey(recoveryProperties.getEmail().buildPrefix(user.getEmail()))).isFalse();
    }

    @Test
    void resetPassword_shouldReturnGone_whenTokenExpired() throws Exception {
        Integer otp = 123456;
        String newPassword = "newTestPassword1234@";
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().otp(otp).password(newPassword).build();
        redisService.saveValue(
                recoveryProperties.getOtp().buildPrefix(String.valueOf(otp)),
                user.getEmail(),
                1);
        userRepository.save(user);

        waitSafely(1000);

        String responseContent = mockMvc.perform(put("/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("Otp has expired.");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();
        assertThat(passwordEncoder.matches(newPassword, optionalUser.get().getPassword())).isFalse();
        assertThat(redisService.hasActiveKey(recoveryProperties.getOtp().buildPrefix(String.valueOf(otp)))).isFalse();
        assertThat(redisService.hasActiveKey(recoveryProperties.getEmail().buildPrefix(user.getEmail()))).isFalse();
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenUserNotFound() throws Exception {
        Integer otp = 123456;
        String newPassword = "newTestPassword1234@";
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().otp(otp).password(newPassword).build();
        redisService.saveValue(
                recoveryProperties.getOtp().buildPrefix(String.valueOf(otp)),
                user.getEmail(),
                recoveryProperties.getOtp().getTtl()
        );

        String responseContent = mockMvc.perform(put("/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("User not found.");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isNotPresent();
    }

    @Test
    void resendOtp_shouldResendOtp_whenFirstAttemptAndOtpAlreadyExists() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        redisService.saveValue(
                recoveryProperties.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                recoveryProperties.getEmail().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(put("/password/resend").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(recoveryProperties.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(1);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldReturnBadRequest_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        redisService.saveValue(
                recoveryProperties.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                recoveryProperties.getEmail().getTtl()
        );

        String responseContent = mockMvc.perform(put("/password/resend").param("email", user.getEmail())
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
        Optional<String> attempts = redisService.getValue(recoveryProperties.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isEmpty();
    }

    @Test
    void resendOtp_shouldResendOtp_whenSecondAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        redisService.saveValue(
                recoveryProperties.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                recoveryProperties.getEmail().getTtl()
        );
        redisService.saveValue(
                recoveryProperties.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(1),
                recoveryProperties.getAttempts().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(put("/password/resend").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(recoveryProperties.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(2);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldResendOtp_whenThirdAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        redisService.saveValue(
                recoveryProperties.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                recoveryProperties.getEmail().getTtl()
        );
        redisService.saveValue(
                recoveryProperties.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(2),
                recoveryProperties.getAttempts().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(put("/password/resend").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(recoveryProperties.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(3);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldReturnTooManyRequests_whenFourthAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        redisService.saveValue(
                recoveryProperties.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                recoveryProperties.getEmail().getTtl()
        );
        redisService.saveValue(
                recoveryProperties.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(3),
                recoveryProperties.getAttempts().getTtl()
        );
        userRepository.save(user);

        String responseContent = mockMvc.perform(put("/password/resend").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Reached maximum attempts for resending otp code.");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(recoveryProperties.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(3);

        await().atMost(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isFalse());
    }

    @Test
    void resendOtp_shouldReturnGone_whenResetSessionExpired() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        userRepository.save(user);

        String responseContent = mockMvc.perform(put("/password/resend").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Reset session expired.");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> otp = redisService.getValue(recoveryProperties.getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isNotPresent();
    }


    @Test
    void resendOtp_shouldBeAbleToResendOtpAfterFourthAttempt_whenTimeoutEnded() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        RedisProperties.PasswordRecovery recoveryProperties = redisProperties.getPasswordRecovery();
        redisService.saveValue(
                recoveryProperties.getEmail().buildPrefix(user.getEmail()),
                String.valueOf(otp),
                recoveryProperties.getEmail().getTtl()
        );
        redisService.saveValue(
                recoveryProperties.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(3),
                1
        );
        userRepository.save(user);

        waitSafely(1000);

        mockMvc.perform(put("/password/resend").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(recoveryProperties.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(1);

        Long attemptsExpire = redisService.getExpire(recoveryProperties.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attemptsExpire).isNotNull();
        assertThat(attemptsExpire ).isCloseTo(recoveryProperties.getAttempts().getTtl(), Percentage.withPercentage(5));

        await().atMost(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isFalse());
    }
}
