package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.builder.AuthRequestDataBuilder;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.common.type.UserRole;
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
}
