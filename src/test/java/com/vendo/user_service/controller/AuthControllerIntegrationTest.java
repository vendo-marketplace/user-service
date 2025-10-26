package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.builder.AuthRequestDataBuilder;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.integration.redis.common.dto.ValidateRequest;
import com.vendo.user_service.integration.redis.common.namespace.otp.EmailVerificationOtpNamespace;
import com.vendo.user_service.integration.redis.service.RedisService;
import com.vendo.user_service.integration.kafka.consumer.TestConsumer;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.security.common.helper.JwtHelper;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.AuthResponse;
import com.vendo.user_service.web.dto.RefreshRequest;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TestConsumer testConsumer;

    @Autowired
    private RedisService redisService;

    @Autowired
    private EmailVerificationOtpNamespace emailVerificationOtpNamespace;

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
    void signUp_shouldSuccessfullyRegisterUser() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();

        mockMvc.perform(post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        ).andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(authRequest.email());
        assertThat(optionalUser).isPresent();
        User user = optionalUser.get();

        assertThat(user.getEmail()).isEqualTo(authRequest.email());
        assertThat(passwordEncoder.matches(authRequest.password(), user.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.INCOMPLETE);
    }

    @Test
    void signUp_shouldReturnBadRequest_whenUserAlreadyExists() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();
        User user = User.builder()
                .email(authRequest.email())
                .password(passwordEncoder.encode(authRequest.password()))
                .build();
        userRepository.save(user);

        MockHttpServletResponse response = mockMvc.perform(post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        ).andExpect(status().isConflict()).andReturn().getResponse();

        String responseMessage = response.getContentAsString();
        assertThat(responseMessage).isNotBlank();
        assertThat(responseMessage).isEqualTo("User with this email already exists.");
    }

    @Test
    void signIn_shouldReturnPairOfTokens() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();
        User user = User.builder()
                .email(authRequest.email())
                .password(passwordEncoder.encode(authRequest.password()))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        MockHttpServletResponse response = mockMvc.perform(post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        ).andExpect(status().isOk()).andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();

        AuthResponse authResponse = objectMapper.readValue(responseContent, AuthResponse.class);
        assertThat(authResponse).isNotNull();

        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(jwtHelper.extractAllClaims(authResponse.accessToken())).isNotNull();

        assertThat(authResponse.refreshToken()).isNotBlank();
        assertThat(jwtHelper.extractAllClaims(authResponse.accessToken())).isNotNull();
    }

    @Test
    void signIn_shouldReturnBadRequest_whenUserDoesNotExist() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();

        MockHttpServletResponse response = mockMvc.perform(post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        ).andExpect(status().isBadRequest()).andReturn().getResponse();

        String responseMessage = response.getContentAsString();
        assertThat(responseMessage).isNotBlank();
        assertThat(responseMessage).isEqualTo("User not found.");
    }

    @Test
    void signIn_shouldReturnForbidden_whenUserBlocked() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();
        User user = User.builder()
                .email(authRequest.email())
                .password(passwordEncoder.encode(authRequest.password()))
                .role(UserRole.USER)
                .status(UserStatus.BLOCKED)
                .build();
        userRepository.save(user);

        MockHttpServletResponse response = mockMvc.perform(post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        ).andExpect(status().isForbidden()).andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("User is unactive.");
    }

    @Test
    void signIn_shouldReturnForbidden_whenUserIncomplete() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();
        User user = User.builder()
                .email(authRequest.email())
                .password(passwordEncoder.encode(authRequest.password()))
                .role(UserRole.USER)
                .status(UserStatus.INCOMPLETE)
                .build();
        userRepository.save(user);

        MockHttpServletResponse response = mockMvc.perform(post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        ).andExpect(status().isForbidden()).andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("User is unactive.");
    }

    @Test
    void refresh_shouldReturnPairOfTokens() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        RefreshRequest refreshRequest = RefreshRequest.builder().refreshToken(refreshToken).build();

        MockHttpServletResponse response = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
        ).andExpect(status().isOk()).andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();

        AuthResponse authResponse = objectMapper.readValue(responseContent, AuthResponse.class);
        assertThat(authResponse).isNotNull();

        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(jwtHelper.extractAllClaims(authResponse.accessToken())).isNotNull();

        assertThat(authResponse.refreshToken()).isNotBlank();
        assertThat(jwtHelper.extractAllClaims(authResponse.accessToken())).isNotNull();
    }

    @Test
    void refresh_shouldReturnForbidden_whenUserDoesNotExist() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String refreshToken = jwtService.generateRefreshToken(user);
        RefreshRequest refreshRequest = RefreshRequest.builder().refreshToken(refreshToken).build();

        MockHttpServletResponse response = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
        ).andExpect(status().isBadRequest()).andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("User does not exist");
    }

    @Test
    void refresh_shouldReturnForbidden_whenTokenIsNotValid() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        String expiredRefreshToken = jwtService.generateTokenWithExpiration(user, 0);
        RefreshRequest refreshRequest = RefreshRequest.builder().refreshToken(expiredRefreshToken).build();

        MockHttpServletResponse response = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
        ).andExpect(status().isUnauthorized()).andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("Token has expired");
    }

    @Test
    void sendOtp_shouldSendEmailVerificationEventSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/send-otp").param("email", user.getEmail())
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

        String responseContent = mockMvc.perform(post("/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Otp has already sent to the email.");

        Optional<String> otpOptional = redisService.getValue(emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()));
        assertThat(otpOptional).isPresent();

        await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.removeIfReceived(otpOptional.get())).isFalse());
    }

    @Test
    void sendOtp_shouldReturnBadRequest_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        String responseContent = mockMvc.perform(post("/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON).param("email", user.getEmail()))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("User not found.");

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

        mockMvc.perform(post("/auth/resend-otp").param("email", user.getEmail())
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

        mockMvc.perform(post("/auth/resend-otp").param("email", user.getEmail())
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
    void resendOtp_shouldReturnBadRequest_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String otp = "123456";
        redisService.saveValue(
                emailVerificationOtpNamespace.getEmail().buildPrefix(user.getEmail()),
                otp,
                emailVerificationOtpNamespace.getEmail().getTtl()
        );

        String responseContent = mockMvc.perform(post("/auth/resend-otp").param("email", user.getEmail())
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

        mockMvc.perform(post("/auth/resend-otp").param("email", user.getEmail())
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

        mockMvc.perform(post("/auth/resend-otp").param("email", user.getEmail())
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

        String responseContent = mockMvc.perform(post("/auth/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Reached maximum attempts for resending otp code.");

        Optional<String> attemptsOptional = redisService.getValue(emailVerificationOtpNamespace.getAttempts().buildPrefix(user.getEmail()));
        assertThat(attemptsOptional).isPresent();
        assertThat(Integer.parseInt(attemptsOptional.get())).isEqualTo(3);

        await().atMost(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testConsumer.removeIfReceived(user.getEmail())).isFalse());
    }

    @Test
    void resendOtp_shouldReturnGone_whenOtpSessionExpired() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);

        String responseContent = mockMvc.perform(post("/auth/resend-otp").param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Otp session expired.");

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

        ValidateRequest validateRequest = ValidateRequest.builder()
                .otp(otp)
                .email(user.getEmail()).build();

        mockMvc.perform(post("/auth/validate")
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
    void validate_shouldReturnBadRequest_whenOtpDoesNotMatchEmail() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        String otp = "123456";
        String mismatchEmail = "mismatch@example.com";

        redisService.saveValue(
                emailVerificationOtpNamespace.getOtp().buildPrefix(otp),
                mismatchEmail,
                emailVerificationOtpNamespace.getOtp().getTtl());

        ValidateRequest validateRequest = ValidateRequest.builder()
                .otp(otp)
                .email(user.getEmail()).build();

        String responseContent = mockMvc.perform(post("/auth/validate")
                        .content(objectMapper.writeValueAsString(validateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Invalid otp.");

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

        ValidateRequest validateRequest = ValidateRequest.builder()
                .otp(otp)
                .email(user.getEmail())
                .build();

        String responseContent = mockMvc.perform(post("/auth/validate")
                        .content(objectMapper.writeValueAsString(validateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseContent).isNotNull();
        assertThat(responseContent).isEqualTo("Otp has expired.");

        User updateUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(updateUser.getStatus()).isEqualTo(UserStatus.INCOMPLETE);
    }
}
