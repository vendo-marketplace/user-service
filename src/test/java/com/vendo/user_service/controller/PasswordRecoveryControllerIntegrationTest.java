package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
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
    void forgotPassword_shouldSendForgotPasswordEventSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);

        mockMvc.perform(post("/password/forgot").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> otp = redisService.getValue(redisProperties.getPasswordRecovery().getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isPresent();
        assertThat(otp.get()).isNotBlank();
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void forgotPassword_shouldReturnConflict_whenForgotPasswordEventHasAlreadySent() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        redisService.saveValue(
                redisProperties.getPasswordRecovery().getEmail().buildPrefix(user.getEmail()),
                String.valueOf(123456),
                redisProperties.getPasswordRecovery().getEmail().getTtl()
        );

        String responseContent = mockMvc.perform(post("/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("Otp has already sent to email");

        Optional<String> otp = redisService.getValue(redisProperties.getPasswordRecovery().getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isPresent();

        assertThat(otp.get()).isNotBlank();
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(otp.get())).isFalse());
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
        assertThat(responseContent).isEqualTo("User not found");

        Optional<String> otp = redisService.getValue(redisProperties.getPasswordRecovery().getEmail().buildPrefix((user.getEmail())));
        assertThat(otp).isEmpty();
    }

    @Test
    void resetPassword_shouldResetPasswordSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        String newPassword = "newTestPassword1234@";
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder()
                .otp(otp)
                .password(newPassword).build();
        userRepository.save(user);
        redisService.saveValue(
                redisProperties.getPasswordRecovery().getOtp().buildPrefix(String.valueOf(otp)),
                user.getEmail(),
                redisProperties.getPasswordRecovery().getOtp().getTtl()
        );

        mockMvc.perform(put("/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();
        assertThat(passwordEncoder.matches(newPassword, optionalUser.get().getPassword())).isTrue();
        assertThat(redisService.hasActiveKey(redisProperties.getPasswordRecovery().getOtp().buildPrefix(String.valueOf(otp)))).isFalse();
        assertThat(redisService.hasActiveKey(redisProperties.getPasswordRecovery().getEmail().buildPrefix(user.getEmail()))).isFalse();
    }

    @Test
    void resetPassword_shouldReturnGone_whenTokenExpired() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        String newPassword = "newTestPassword1234@";
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().otp(otp).password(newPassword).build();
        userRepository.save(user);
        redisService.saveValue(
                redisProperties.getPasswordRecovery().getOtp().buildPrefix(String.valueOf(otp)),
                user.getEmail(),
                1);

        waitSafely(1000);

        String responseContent = mockMvc.perform(put("/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isGone()).andReturn().getResponse().getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("Otp has expired");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();
        assertThat(passwordEncoder.matches(newPassword, optionalUser.get().getPassword())).isFalse();
        assertThat(redisService.hasActiveKey(redisProperties.getPasswordRecovery().getOtp().buildPrefix(String.valueOf(otp)))).isFalse();
        assertThat(redisService.hasActiveKey(redisProperties.getPasswordRecovery().getEmail().buildPrefix(user.getEmail()))).isFalse();
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        Integer otp = 123456;
        String newPassword = "newTestPassword1234@";
        ResetPasswordRequest resetPasswordRequest = ResetPasswordRequest.builder().otp(otp).password(newPassword).build();
        redisService.saveValue(
                redisProperties.getPasswordRecovery().getOtp().buildPrefix(String.valueOf(otp)),
                user.getEmail(),
                redisProperties.getPasswordRecovery().getOtp().getTtl()
        );

        String responseContent = mockMvc.perform(put("/password/reset")
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

//    @Test
//    void resendOtp_shouldResendOtp_whenFirstAttemptAndOtpAlreadyExists() throws Exception {
//        User user = UserDataBuilder.buildUserWithRequiredFields().build();
//        Integer otp = 123456;
//        redisService.saveValue(
//                redisProperties.getPasswordRecovery().getEmail().buildPrefix(user.getEmail()),
//                String.valueOf(otp),
//                redisProperties.getPasswordRecovery().getEmail().getTtl()
//        );
//
//        mockMvc.perform(put("/password/resend").param("email", user.getEmail())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk());
//
//        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
//        assertThat(optionalUser).isNotPresent();
//    }

    @Test
    void resendOtp_shouldGenerateNewOtp_whenFirstAttemptAndOtpExpired() {

    }

    @Test
    void resendOtp_shouldResendOtp_whenSecondAttempt() {

    }

    @Test
    void resendOtp_shouldResendOtp_whenThirdAttempt() {

    }

    @Test
    void resendOtp_shouldSkipResendingOtp_whenFourthAttemptOrMore() {

    }

    @Test
    void resendOtp_shouldBeAbleToResendOtpAfterFourthAttempt_whenTimeoutEnded() {

    }

    @Test
    void resendOtp_shouldThrowException_whenUserNotFound() {

    }

}
