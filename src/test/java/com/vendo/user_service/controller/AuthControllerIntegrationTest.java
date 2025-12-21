package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.common.exception.ExceptionResponse;
import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.builder.AuthRequestDataBuilder;
import com.vendo.user_service.common.builder.CompleteAuthRequestDataBuilder;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.db.model.User;
import com.vendo.user_service.db.repository.UserRepository;
import com.vendo.user_service.security.common.helper.JwtHelper;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.web.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static com.vendo.security.common.constants.AuthConstants.BEARER_PREFIX;
import static com.vendo.user_service.security.common.util.SecurityContextUtils.initializeSecurityContext;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        flushRedis();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        flushRedis();
        userRepository.deleteAll();
    }

    private void flushRedis() {
        RedisConnection connection = redisTemplate.getRequiredConnectionFactory().getConnection();
        connection.flushAll();
        connection.close();
    }

    @Test
    void signUp_shouldSuccessfullyRegisterUser() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().build();

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(authRequest.email());
        assertThat(optionalUser).isPresent();
        User user = optionalUser.get();

        assertThat(user.getEmail()).isEqualTo(authRequest.email());
        assertThat(passwordEncoder.matches(authRequest.password(), user.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.INCOMPLETE);
        assertThat(user.getProviderType()).isEqualTo(ProviderType.LOCAL);
    }

    @Test
    void signUp_shouldReturnConflict_whenUserAlreadyExists() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().build();
        User user = UserDataBuilder.buildUserAllFields()
                .email(authRequest.email())
                .password(passwordEncoder.encode(authRequest.password()))
                .build();
        userRepository.save(user);

        String content = mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotBlank();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse.getMessage()).isEqualTo("User already exists.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/sign-up");
    }

    @Test
    void signIn_shouldReturnPairOfTokens() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().build();
        User user = UserDataBuilder.buildUserAllFields()
                .status(UserStatus.ACTIVE)
                .email(authRequest.email())
                .password(passwordEncoder.encode(authRequest.password()))
                .build();
        userRepository.save(user);

        String content = mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotBlank();
        AuthResponse authResponse = objectMapper.readValue(content, AuthResponse.class);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(jwtHelper.extractAllClaims(authResponse.accessToken())).isNotNull();

        assertThat(authResponse.refreshToken()).isNotBlank();
        assertThat(jwtHelper.extractAllClaims(authResponse.accessToken())).isNotNull();
    }

    @Test
    void signIn_shouldReturnNotFound_whenUserNotFound() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().build();

        MockHttpServletResponse response = mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isNotFound()).andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.getMessage()).isEqualTo("User not found.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/sign-in");
    }

    @Test
    void signIn_shouldReturnForbidden_whenUserBlocked() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().build();
        User user = UserDataBuilder.buildUserAllFields()
                .status(UserStatus.BLOCKED)
                .email(authRequest.email())
                .password(passwordEncoder.encode(authRequest.password()))
                .build();
        userRepository.save(user);

        String content = mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotBlank();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse.getMessage()).isEqualTo("User is unactive.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/sign-in");
    }

    @Test
    void signIn_shouldReturnForbidden_whenUserIncomplete() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().build();
        User user = UserDataBuilder.buildUserAllFields()
                .status(UserStatus.INCOMPLETE)
                .email(authRequest.email())
                .password(passwordEncoder.encode(authRequest.password()))
                .build();
        userRepository.save(user);

        String content = mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotBlank();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse.getMessage()).isEqualTo("User is unactive.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/sign-in");
    }

    @Test
    void refresh_shouldReturnPairOfTokens() throws Exception {
        User user = UserDataBuilder.buildUserAllFields().build();
        userRepository.save(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        RefreshRequest refreshRequest = RefreshRequest.builder().refreshToken(BEARER_PREFIX + refreshToken).build();

        String content = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotBlank();
        AuthResponse authResponse = objectMapper.readValue(content, AuthResponse.class);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(jwtHelper.extractAllClaims(authResponse.accessToken())).isNotNull();

        assertThat(authResponse.refreshToken()).isNotBlank();
        assertThat(jwtHelper.extractAllClaims(authResponse.accessToken())).isNotNull();
    }

    @Test
    void refresh_shouldReturnNotFound_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserAllFields().build();
        String refreshToken = jwtService.generateRefreshToken(user);
        RefreshRequest refreshRequest = RefreshRequest.builder().refreshToken(BEARER_PREFIX + refreshToken).build();

        String content = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotBlank();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse.getMessage()).isEqualTo("User not found.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/refresh");
    }

    @Test
    void refresh_shouldReturnUnauthorized_whenTokenWithoutBearerPrefix() throws Exception {
        User user = UserDataBuilder.buildUserAllFields().build();
        userRepository.save(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        refreshToken = refreshToken.substring(BEARER_PREFIX.length() + 1);

        RefreshRequest refreshRequest = RefreshRequest.builder().refreshToken(refreshToken).build();

        String content = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotBlank();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse.getMessage()).isEqualTo("Invalid token.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/refresh");
    }

    @Test
    void refresh_shouldReturnUnauthorized_whenTokenIsExpired() throws Exception {
        User user = UserDataBuilder.buildUserAllFields().build();
        userRepository.save(user);
        String expiredRefreshToken = jwtService.generateTokenWithExpiration(user, 0);
        RefreshRequest refreshRequest = RefreshRequest.builder().refreshToken(BEARER_PREFIX + expiredRefreshToken).build();

        String content = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotBlank();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse.getMessage()).isEqualTo("Token has expired.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/refresh");
    }

    @Test
    void completeAuth_shouldSuccessfullyCompleteRegistration() throws Exception {
        User user = UserDataBuilder.buildUserAllFields().emailVerified(true).build();
        userRepository.save(user);
        CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields().build();

        mockMvc.perform(patch("/auth/complete-auth")
                        .param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeAuthRequest)))
                .andExpect(status().isOk());

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        assertThat(optionalUser).isPresent();
        assertThat(optionalUser.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(optionalUser.get().getFullName()).isEqualTo(completeAuthRequest.fullName());
        assertThat(optionalUser.get().getBirthDate()).isEqualTo(completeAuthRequest.birthDate());
    }

    @Test
    void completeProfile_shouldReturnBadRequest_whenNotValidFullName() throws Exception {
        User user = UserDataBuilder.buildUserAllFields().build();
        userRepository.save(user);
        CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields()
                .fullName("Invalid_fullName")
                .build();

        String content = mockMvc.perform(patch("/auth/complete-auth")
                        .param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeAuthRequest)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotNull();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/complete-auth");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(exceptionResponse.getErrors()).isNotNull();
        assertThat(exceptionResponse.getErrors().size()).isEqualTo(1);
        assertThat(exceptionResponse.getErrors().get("fullName")).isNotNull();
    }

    @Test
    void completeProfile_shouldReturnBadRequest_whenNotAdult() throws Exception {
        User user = UserDataBuilder.buildUserAllFields().build();
        userRepository.save(user);
        CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields()
                .birthDate(LocalDate.now())
                .build();

        String content = mockMvc.perform(patch("/auth/complete-auth")
                        .param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeAuthRequest)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotNull();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/complete-auth");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(exceptionResponse.getErrors()).isNotNull();
        assertThat(exceptionResponse.getErrors().size()).isEqualTo(1);
        assertThat(exceptionResponse.getErrors().get("birthDate")).isNotNull();
    }

    @Test
    void completeProfile_shouldReturnBadRequest_whenBothNotAdultAndInvalidFullName() throws Exception {
        User user = UserDataBuilder.buildUserAllFields().build();
        userRepository.save(user);
        CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields()
                .fullName("Invalid_fullName")
                .birthDate(LocalDate.of(2025, 1, 1))
                .build();

        String content = mockMvc.perform(patch("/auth/complete-auth")
                        .param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeAuthRequest)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotNull();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/complete-auth");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(exceptionResponse.getErrors()).isNotNull();
        assertThat(exceptionResponse.getErrors().size()).isEqualTo(2);
        assertThat(exceptionResponse.getErrors().get("birthDate")).isNotNull();
        assertThat(exceptionResponse.getErrors().get("fullName")).isNotNull();
    }

    @Test
    void completeProfile_shouldReturnNotFound_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserAllFields().build();
        CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields().build();

        String content = mockMvc.perform(patch("/auth/complete-auth")
                        .param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeAuthRequest)))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotNull();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/complete-auth");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.getMessage()).isEqualTo("User not found.");
    }

    @Test
    void completeProfile_shouldReturnForbidden_whenUserBlocked() throws Exception {
        User user = UserDataBuilder.buildUserAllFields()
                .status(UserStatus.BLOCKED)
                .emailVerified(true)
                .build();
        userRepository.save(user);
        CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields().build();

        String content = mockMvc.perform(patch("/auth/complete-auth")
                        .param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeAuthRequest)))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotNull();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/complete-auth");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(exceptionResponse.getMessage()).isEqualTo("Your account is blocked.");
    }

    @Test
    void completeProfile_shouldReturn_whenUserAlreadyCompletedRegistration() throws Exception {
        User user = UserDataBuilder.buildUserAllFields()
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields().build();

        String content = mockMvc.perform(patch("/auth/complete-auth")
                        .param("email", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeAuthRequest)))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotNull();
        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/complete-auth");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(exceptionResponse.getErrors()).isNull();
        assertThat(exceptionResponse.getMessage()).isEqualTo("Your account is already activated.");
    }

    @Test
    void getAuthenticatedUser_shouldReturnUserProfile() throws Exception {
        User user = UserDataBuilder.buildUserAllFields()
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        SecurityContext securityContext = initializeSecurityContext(user);

        String content = mockMvc.perform(get("/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.securityContext(securityContext)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).isNotNull();
        UserProfileResponse responseDto = objectMapper.readValue(content, UserProfileResponse.class);

        assertThat(content).doesNotContain("password");
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.id()).isEqualTo("1");
        assertThat(responseDto.email()).isEqualTo("test@gmail.com");
        assertThat(responseDto.fullName()).isEqualTo("Test Name");
        assertThat(responseDto.role()).isEqualTo(UserRole.USER);
        assertThat(responseDto.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(responseDto.providerType()).isEqualTo(ProviderType.LOCAL);
        assertThat(responseDto.createdAt()).isNotNull();
        assertThat(responseDto.updatedAt()).isNotNull();
    }

    @Test
    void getAuthenticatedUser_shouldReturnUnauthorized_whenNotUserInstance() throws Exception {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "not-a-user-object",
                null,
                null);
        SecurityContext securityContext = initializeSecurityContext(authToken);

        String content = mockMvc.perform(get("/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.securityContext(securityContext)))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/me");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getMessage()).isEqualTo("Unauthorized.");
    }

    @Test
    void getAuthenticatedUser_shouldReturnNotFound_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserAllFields()
                .status(UserStatus.ACTIVE)
                .build();
        SecurityContext securityContext = initializeSecurityContext(user);

        String content = mockMvc.perform(get("/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.securityContext(securityContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getPath()).isEqualTo("/auth/me");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.getMessage()).isEqualTo("User not found.");
    }
}
