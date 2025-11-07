package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.common.exception.ExceptionResponse;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.system.redis.common.dto.ResetPasswordRequest;
import com.vendo.user_service.system.redis.common.namespace.otp.PasswordRecoveryOtpNamespace;
import com.vendo.user_service.system.redis.service.RedisService;
import com.vendo.user_service.integration.kafka.consumer.TestConsumer;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    private PasswordRecoveryOtpNamespace passwordRecoveryOtpNamespace;

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
    void forgotPassword_shouldSendForgotPasswordEventSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);

        mockMvc.perform(post("/password/forgot").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> otp = redisService.getValue(passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isPresent();
        assertThat(otp.get().length()).isEqualTo(6);

        Optional<String> email = redisService.getValue(passwordRecoveryOtpNamespace.getOtp().buildPrefix(otp.get()));
        assertThat(email).isPresent();
        assertThat(email.get()).isEqualTo(user.getEmail());

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void forgotPassword_shouldReturnConflict_whenForgotPasswordEventHasAlreadySent() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";

        redisService.saveValue(
                passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                passwordRecoveryOtpNamespace.getEmail().getTtl()
        );
        userRepository.save(user);

        String responseContent = mockMvc.perform(post("/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Otp has already sent to the email.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.CONFLICT.value());

        Optional<String> otpOptional = redisService.getValue(passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()));
        assertThat(otpOptional).isPresent();

        await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.removeIfReceived(otpOptional.get())).isFalse());
    }

    @Test
    void forgotPassword_shouldReturnNotFound_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        String responseContent = mockMvc.perform(post("/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User not found.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.NOT_FOUND.value());

        Optional<String> otp = redisService.getValue(passwordRecoveryOtpNamespace.getEmail().buildPrefix((user.getEmail())));
        assertThat(otp).isEmpty();
    }

    @Test
    void resetPassword_shouldResetPassword() throws Exception {
        String otp = "123456";
        String newPassword = "newTestPassword1234@";
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder()
                .password(newPassword).build();
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getOtp().buildPrefix(otp),
                user.getEmail(),
                passwordRecoveryOtpNamespace.getOtp().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(put("/password/reset").param("otp", otp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        assertThat(passwordEncoder.matches(newPassword, optionalUser.get().getPassword())).isTrue();
        assertThat(redisService.hasActiveKey(passwordRecoveryOtpNamespace.getOtp().buildPrefix(otp))).isFalse();
        assertThat(redisService.hasActiveKey(passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()))).isFalse();
    }

    @Test
    void resetPassword_shouldReturnGone_whenTokenExpired() throws Exception {
        String otp = "123456";
        String newPassword = "newTestPassword1234@";
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().password(newPassword).build();
        userRepository.save(user);

        String responseContent = mockMvc.perform(put("/password/reset").param("otp", otp)
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
    }

    @Test
    void resetPassword_shouldReturnNotFound_whenUserNotFound() throws Exception {
        String otp = "123456";
        String newPassword = "newTestPassword1234@";
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().password(newPassword).build();
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getOtp().buildPrefix(otp),
                user.getEmail(),
                passwordRecoveryOtpNamespace.getOtp().getTtl()
        );

        String responseContent = mockMvc.perform(put("/password/reset").param("otp", otp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User not found.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.NOT_FOUND.value());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isNotPresent();
    }

    @Test
    void resendOtp_shouldResendOtp_whenFirstAttempt_andOtpAlreadyExists() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                passwordRecoveryOtpNamespace.getEmail().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(put("/password/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> attempts = redisService.getValue(passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(1);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldResendOtp_whenFirstAttempt_andOtpDoesNotExist() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                passwordRecoveryOtpNamespace.getEmail().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(put("/password/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> attempts = redisService.getValue(passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(1);

        Long attemptsExpire = redisService.getExpire(passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attemptsExpire).isNotNull();
        assertThat(attemptsExpire).isCloseTo(passwordRecoveryOtpNamespace.getAttempts().getTtl(), Percentage.withPercentage(5));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldReturnNotFound_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                passwordRecoveryOtpNamespace.getEmail().getTtl()
        );

        String responseContent = mockMvc.perform(put("/password/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User not found.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.NOT_FOUND.value());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isNotPresent();

        await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isFalse());
        Optional<String> attempts = redisService.getValue(passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isEmpty();
    }

    @Test
    void resendOtp_shouldResendOtp_whenSecondAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                passwordRecoveryOtpNamespace.getEmail().getTtl()
        );
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(1),
                passwordRecoveryOtpNamespace.getAttempts().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(put("/password/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> attempts = redisService.getValue(passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(2);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldResendOtp_whenThirdAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                passwordRecoveryOtpNamespace.getEmail().getTtl()
        );
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(2),
                passwordRecoveryOtpNamespace.getAttempts().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(put("/password/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> attempts = redisService.getValue(passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(3);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldReturnTooManyRequests_whenFourthAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                passwordRecoveryOtpNamespace.getEmail().getTtl()
        );
        redisService.saveValue(
                passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()),
                String.valueOf(3),
                passwordRecoveryOtpNamespace.getAttempts().getTtl()
        );
        userRepository.save(user);

        String responseContent = mockMvc.perform(put("/password/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Reached maximum attempts for resending otp code.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        Optional<String> attempts = redisService.getValue(passwordRecoveryOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(3);

        await().atMost(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isFalse());
    }

    @Test
    void resendOtp_shouldReturnGone_whenOtpSessionExpired() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);

        String responseContent = mockMvc.perform(put("/password/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Otp session expired.");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        Optional<String> otp = redisService.getValue(passwordRecoveryOtpNamespace.getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isNotPresent();
    }
}
