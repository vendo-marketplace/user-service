package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.common.exception.ExceptionResponse;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.integration.redis.common.exception.OtpExpiredException;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.common.exception.OtpAlreadySentException;
import com.vendo.user_service.common.exception.TooManyOtpRequestsException;
import com.vendo.user_service.integration.kafka.consumer.TestConsumer;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.system.redis.common.dto.ValidateRequest;
import com.vendo.user_service.system.redis.common.namespace.otp.EmailVerificationOtpNamespace;
import com.vendo.user_service.system.redis.service.RedisService;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
public class EmailVerificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestConsumer testConsumer;

    @Autowired
    private RedisService redisService;

    @Autowired
    private EmailVerificationOtpNamespace emailVerificationOtpNamespace;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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
    void sendOtp_shouldSendEmailVerificationEventSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/verification/send-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> otp = redisService.getValue(emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isPresent();
        assertThat(otp.get().length()).isEqualTo(6);

        Optional<String> email = redisService.getValue(emailVerificationOtpNamespace.getOtp().buildPrefix(otp.get()));
        assertThat(email).isPresent();
        assertThat(email.get()).isEqualTo(user.getEmail());

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void sendOtp_shouldReturnConflict_whenEmailVerificationEventHasAlreadySent() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";

        redisService.saveValue(
                emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                emailVerificationOtpNamespace.getEmail().getTtl()
        );
        userRepository.save(user);

        String responseContent = mockMvc.perform(post("/verification/send-otp")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Otp has already sent.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(exceptionResponse.path()).isEqualTo("/verification/send-otp");
        assertThat(exceptionResponse.type()).isEqualTo(OtpAlreadySentException.class.getSimpleName());

        Optional<String> otpOptional = redisService.getValue(emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()));
        assertThat(otpOptional).isPresent();

        await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.removeIfReceived(otpOptional.get())).isFalse());
    }

    @Test
    void sendOtp_shouldReturnNotFound_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        String responseContent = mockMvc.perform(post("/verification/send-otp")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User not found.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.path()).isEqualTo("/verification/send-otp");
        assertThat(exceptionResponse.type()).isEqualTo(UsernameNotFoundException.class.getSimpleName());

        Optional<String> otp = redisService.getValue(emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isEmpty();
    }

    @Test
    void resendOtp_shouldResendOtp_whenFirstAttemptAndOtpAlreadyExist() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                emailVerificationOtpNamespace.getEmail().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(post("/verification/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> attempts = redisService.getValue(emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(1);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldResendOtp_whenFirstAttemptAndOtpDoesNotExist() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                emailVerificationOtpNamespace.getEmail().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(post("/verification/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> attempts = redisService.getValue(emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isPresent();
        assertThat(Integer.parseInt(attempts.get())).isEqualTo(1);

        Long attemptsExpire = redisService.getExpire(emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attemptsExpire).isNotNull();
        assertThat(attemptsExpire).isCloseTo(emailVerificationOtpNamespace.getAttempts().getTtl(), Percentage.withPercentage(5));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldReturnNotFound_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                emailVerificationOtpNamespace.getEmail().getTtl()
        );

        String responseContent = mockMvc.perform(post("/verification/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User not found.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.path()).isEqualTo("/verification/resend-otp");
        assertThat(exceptionResponse.type()).isEqualTo(UsernameNotFoundException.class.getSimpleName());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isNotPresent();

        await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isFalse());
        Optional<String> attempts = redisService.getValue(emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attempts).isEmpty();
    }

    @Test
    void resendOtp_shouldResendOtp_whenSecondAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        String attempts = "1";
        redisService.saveValue(
                emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                emailVerificationOtpNamespace.getEmail().getTtl()
        );
        redisService.saveValue(
                emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()),
                attempts,
                emailVerificationOtpNamespace.getAttempts().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(post("/verification/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> attemptsOptional = redisService.getValue(emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attemptsOptional).isPresent();
        assertThat(Integer.parseInt(attemptsOptional.get())).isEqualTo(2);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldResendOtp_whenThirdAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        String attempts = "2";
        redisService.saveValue(
                emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                emailVerificationOtpNamespace.getEmail().getTtl()
        );
        redisService.saveValue(
                emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()),
                attempts,
                emailVerificationOtpNamespace.getAttempts().getTtl()
        );
        userRepository.save(user);

        mockMvc.perform(post("/verification/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Optional<String> attemptsOptional = redisService.getValue(emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attemptsOptional).isPresent();
        assertThat(Integer.parseInt(attemptsOptional.get())).isEqualTo(3);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isTrue());
    }

    @Test
    void resendOtp_shouldReturnTooManyRequests_whenFourthAttempt() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        String attempts = "3";
        redisService.saveValue(
                emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                emailVerificationOtpNamespace.getEmail().getTtl()
        );
        redisService.saveValue(
                emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()),
                attempts,
                emailVerificationOtpNamespace.getAttempts().getTtl()
        );
        userRepository.save(user);

        String responseContent = mockMvc.perform(post("/verification/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Reached maximum attempts.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(exceptionResponse.path()).isEqualTo("/verification/resend-otp");
        assertThat(exceptionResponse.type()).isEqualTo(TooManyOtpRequestsException.class.getSimpleName());

        Optional<String> attemptsOptional = redisService.getValue(emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attemptsOptional).isPresent();
        assertThat(Integer.parseInt(attemptsOptional.get())).isEqualTo(3);

        await().atMost(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isFalse());
    }

    @Test
    void resendOtp_shouldReturnGone_whenOtpSessionExpired() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);

        String responseContent = mockMvc.perform(post("/verification/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Otp session expired.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.GONE.value());
        assertThat(exceptionResponse.path()).isEqualTo("/verification/resend-otp");
        assertThat(exceptionResponse.type()).isEqualTo(OtpExpiredException.class.getSimpleName());

        Optional<String> otp = redisService.getValue(emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()));
        assertThat(otp).isNotPresent();
    }

    @Test
    void validate_shouldActivateUser_whenOtpIsValid() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields()
                .status(UserStatus.BLOCKED)
                .build();
        userRepository.save(user);
        String otp = "123456";

        redisService.saveValue(
                emailVerificationOtpNamespace.getOtp().buildPrefix(otp),
                user.getEmail(),
                emailVerificationOtpNamespace.getOtp().getTtl()
        );
        redisService.saveValue(
                emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                emailVerificationOtpNamespace.getEmail().getTtl()
        );

        ValidateRequest validateRequest = ValidateRequest.builder().email(user.getEmail()).build();

        mockMvc.perform(post("/verification/validate").param("otp", otp)
                        .content(objectMapper.writeValueAsString(validateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
            assertThat(optionalUser).isPresent();
            assertThat(optionalUser.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
        });

        assertThat(redisService.getValue(emailVerificationOtpNamespace.getOtp().buildPrefix(otp))).isEmpty();
        assertThat(redisService.getValue(emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()))).isEmpty();
        assertThat(redisService.getValue(emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()))).isEmpty();
    }

    @Test
    void validate_shouldReturnGone_whenOtpDoesNotMatchEmail() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        String otp = "123456";
        String mismatchEmail = "mismatch@example.com";

        redisService.saveValue(
                emailVerificationOtpNamespace.getOtp().buildPrefix(otp),
                mismatchEmail,
                emailVerificationOtpNamespace.getOtp().getTtl());

        ValidateRequest validateRequest = ValidateRequest.builder().email(user.getEmail()).build();

        String responseContent = mockMvc.perform(post("/verification/validate").param("otp", otp)
                        .content(objectMapper.writeValueAsString(validateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Invalid otp.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.GONE.value());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();

        assertThat(optionalUser.get().getStatus()).isEqualTo(UserStatus.INCOMPLETE);
        Optional<String> mismatchEmailOptional = redisService.getValue(emailVerificationOtpNamespace.getOtp().buildPrefix(otp));
        assertThat(mismatchEmailOptional).isPresent();
        assertThat(mismatchEmailOptional).isNotEqualTo(validateRequest.email());
    }

    @Test
    void validate_shouldReturnGone_whenOtpExpired() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        String otp = "123456";

        ValidateRequest validateRequest = ValidateRequest.builder().email(user.getEmail()).build();

        String responseContent = mockMvc.perform(post("/verification/validate").param("otp", otp)
                        .content(objectMapper.writeValueAsString(validateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Otp session expired.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.GONE.value());
        assertThat(exceptionResponse.path()).isEqualTo("/verification/validate");

        User updateUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(updateUser.getStatus()).isEqualTo(UserStatus.INCOMPLETE);
    }
}