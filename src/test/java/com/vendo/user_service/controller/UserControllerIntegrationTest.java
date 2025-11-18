package com.vendo.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.common.exception.ExceptionResponse;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.security.common.exception.InvalidTokenException;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.web.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCurrentUser_shouldReturnUserProfile() throws Exception {

        User loggedUser = UserDataBuilder.buildUserWithRequiredFields()
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(loggedUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        loggedUser, null, loggedUser.getAuthorities()
                )
        );

        String response = mockMvc.perform(get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserProfileResponse responseDto = objectMapper.readValue(response, UserProfileResponse.class);

        assertThat(responseDto.id()).isEqualTo("1");
        assertThat(responseDto.email()).isEqualTo("test@gmail.com");
        assertThat(responseDto.fullName()).isEqualTo("John Doe");
        assertThat(responseDto.createdAt()).isNotNull();
        assertThat(responseDto.updatedAt()).isNotNull();
    }

    @Test
    void getCurrentUser_shouldReturnUnauthorized_whenNoAuthentication_viaMockMvc() throws Exception {
        SecurityContextHolder.clearContext();

        MockHttpServletResponse response = mockMvc.perform(get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse();

        ExceptionResponse exceptionResponse = objectMapper.readValue(response.getContentAsString(), ExceptionResponse.class);

        assertThat(exceptionResponse.message()).isEqualTo("Invalid token.");
        assertThat(exceptionResponse.code()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.path()).isEqualTo("/users/me");
        assertThat(exceptionResponse.type()).isEqualTo(InvalidTokenException.class.getSimpleName());
    }
}
