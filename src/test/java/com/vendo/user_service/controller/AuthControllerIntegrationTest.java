package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.user_service.builder.AuthRequestDataBuilder;
import com.vendo.user_service.builder.UserDataBuilder;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.common.type.UserStatus;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.common.dto.ForgotPasswordRequest;
import com.vendo.user_service.integration.redis.common.dto.ResetPasswordRequest;
import com.vendo.user_service.integration.redis.service.RedisService;
import com.vendo.user_service.kafka.consumer.TestConsumer;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.security.common.helper.JwtHelper;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.AuthResponse;
import com.vendo.user_service.web.dto.RefreshRequest;
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
    private TestConsumer testConsumer;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisProperties redisProperties;

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
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
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
        assertThat(responseMessage).isEqualTo("User with this email already exists");
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
        assertThat(responseMessage).isEqualTo("User not found");
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
        assertThat(responseContent).isEqualTo("User is blocked");

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
    void forgotPassword_shouldSentNotificationEventSuccessfully() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        ForgotPasswordRequest forgotPasswordRequest = ForgotPasswordRequest.builder().email(user.getEmail()).build();

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andExpect(status().isOk());

        Optional<String> target = redisService.getValue(redisProperties.getResetPassword().getPrefixes().getEmailPrefix() + user.getEmail());
        assertThat(target).isPresent();

        String token = target.get();
        assertThat(token).isNotBlank();
        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(testConsumer.hasReceived(token)).isTrue());
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

        String responseContent = mockMvc.perform(post("/auth/forgot-password")
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

        String responseContent = mockMvc.perform(post("/auth/forgot-password")
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

        String responseContent = mockMvc.perform(post("/auth/forgot-password")
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

        mockMvc.perform(put("/auth/reset-password").param("token", token)
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
        String responseContent = mockMvc.perform(put("/auth/reset-password").param("token", token)
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

        String responseContent = mockMvc.perform(put("/auth/reset-password").param("token", token)
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
