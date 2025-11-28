package com.vendo.user_service.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.common.exception.ExceptionResponse;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.security.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class JwtAuthFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        userRepository.deleteAll();
    }

    @Test
    void doFilterInternal_shouldPassAuthorization_whenUserAlreadyAuthorized() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        MockHttpServletResponse response = mockMvc.perform(get("/test/ping"))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        String responseContent = response.getContentAsString();

        assertThat(responseContent).isNotBlank();
        assertThat(responseContent).isEqualTo("pong");
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorized_whenNoTokenInRequest() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/test/ping"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Invalid token.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void doFilterInternal_shouldPassFilter_whenTokenIsValid() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields()
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        String accessToken = jwtService.generateAccessToken(user);

        mockMvc.perform(get("/test/ping").header(AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorized_whenTokenWithoutBearerPrefix() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String accessToken = jwtService.generateAccessToken(user);

        MockHttpServletResponse response = mockMvc.perform(get("/test/ping").header(AUTHORIZATION, accessToken))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Invalid token.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.path()).isEqualTo("/test/ping");
    }

    @Test
    void doFilterInternal_shouldReturnNotFound_whenUserNotFound() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        String accessToken = jwtService.generateAccessToken(user);

        MockHttpServletResponse response = mockMvc.perform(get("/test/ping").header(AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andReturn().getResponse();

        String responseContent = response.getContentAsString();
        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("User not found.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.path()).isEqualTo("/test/ping");
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorized_whenTokenIsNotValid() throws Exception {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        userRepository.save(user);
        String expiredToken = jwtService.generateTokenWithExpiration(user, 0);

        MockHttpServletResponse response = mockMvc.perform(get("/test/ping").header(AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse();

        String responseContent = response.getContentAsString();

        assertThat(responseContent).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(responseContent, ExceptionResponse.class);
        assertThat(exceptionResponse.message()).isEqualTo("Token has expired.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.path()).isEqualTo("/test/ping");
    }
}
