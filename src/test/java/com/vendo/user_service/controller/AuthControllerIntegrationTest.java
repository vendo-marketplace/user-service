package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.common.exception.ExceptionResponse;
import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.security.common.exception.AccessDeniedException;
import com.vendo.user_service.common.builder.AuthRequestDataBuilder;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.security.common.helper.JwtHelper;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.AuthResponse;
import com.vendo.user_service.web.dto.RefreshRequest;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
        assertThat(user.getProviderType()).isEqualTo(ProviderType.LOCAL);
    }

    @Test
    void signUp_shouldReturnConflict_whenUserAlreadyExists() throws Exception {
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

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User already exists.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(exceptionResponse.path()).isEqualTo("/auth/sign-up");
        assertThat(exceptionResponse.type()).isEqualTo(UserAlreadyExistsException.class.getSimpleName());
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
    void signIn_shouldReturnNotFound_whenUserNotFound() throws Exception {
        AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithRequiredFields().build();

        MockHttpServletResponse response = mockMvc.perform(post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        ).andExpect(status().isNotFound()).andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User not found.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.path()).isEqualTo("/auth/sign-in");
        assertThat(exceptionResponse.type()).isEqualTo(UsernameNotFoundException.class.getSimpleName());
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

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User is unactive.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(exceptionResponse.path()).isEqualTo("/auth/sign-in");
        assertThat(exceptionResponse.type()).isEqualTo(AccessDeniedException.class.getSimpleName());
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

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User is unactive.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(exceptionResponse.path()).isEqualTo("/auth/sign-in");
        assertThat(exceptionResponse.type()).isEqualTo(AccessDeniedException.class.getSimpleName());
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
    void refresh_shouldReturnNotFound_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String refreshToken = jwtService.generateRefreshToken(user);
        RefreshRequest refreshRequest = RefreshRequest.builder().refreshToken(refreshToken).build();

        MockHttpServletResponse response = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
        ).andExpect(status().isNotFound()).andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User not found.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.path()).isEqualTo("/auth/refresh");
        assertThat(exceptionResponse.type()).isEqualTo(UsernameNotFoundException.class.getSimpleName());
    }

    @Test
    void refresh_shouldReturnUnauthorized_whenTokenIsNotValid() throws Exception {
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

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Token has expired");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.path()).isEqualTo("/auth/refresh");
        assertThat(exceptionResponse.type()).isEqualTo(ExpiredJwtException.class.getSimpleName());
    }
}
