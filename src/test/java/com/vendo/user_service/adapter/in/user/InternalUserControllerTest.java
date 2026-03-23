package com.vendo.user_service.adapter.in.user;

import com.vendo.core_lib.exception.ExceptionResponse;
import com.vendo.test_utils_lib.AssertionUtils;
import com.vendo.user_lib.exception.UserNotFoundException;
import com.vendo.user_service.adapter.in.user.dto.SaveUserRequest;
import com.vendo.user_service.adapter.in.user.dto.UpdateUserRequest;
import com.vendo.user_service.adapter.out.user.mapper.UserMapper;
import com.vendo.user_service.application.command.ExistsUserResponse;
import com.vendo.user_service.domain.user.SaveUserRequestDataBuilder;
import com.vendo.user_service.domain.user.UpdateUserRequestDataBuilder;
import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.domain.user.UserDataBuilder;
import com.vendo.user_service.port.user.UserCommandPort;
import com.vendo.user_service.port.user.UserQueryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static com.vendo.user_service.adapter.out.security.util.SecurityContextUtils.initAuth;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InternalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserQueryPort userQueryPort;

    @MockitoBean
    private UserCommandPort userCommandPort;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void getByEmail_shouldReturnUser() throws Exception {
        User user = UserDataBuilder.withAllFields().build();

        when(userQueryPort.getByEmail(user.getEmail())).thenReturn(user);

        String content = mockMvc.perform(get("/internal/users")
                        .param("email", user.getEmail())
                        .with(authentication(initAuth(null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();
        User userResponse = objectMapper.readValue(content, User.class);

        AssertionUtils.assertFromDto(user, userResponse);
    }

    @Test
    void getByEmail_shouldReturnNotFound() throws Exception {
        User user = UserDataBuilder.withAllFields().build();

        when(userQueryPort.getByEmail(user.getEmail())).thenThrow(new UserNotFoundException("User not found."));

        String content = mockMvc.perform(get("/internal/users")
                        .param("email", user.getEmail())
                        .with(authentication(initAuth(null, null))))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exceptionResponse.getMessage()).isEqualTo("User not found.");
        assertThat(exceptionResponse.getPath()).isEqualTo("/internal/users");
        assertThat(exceptionResponse.getTimestamp()).isNotNull();
    }

    @Test
    void existsByEmail_shouldReturnExistenceStatus() throws Exception {
        String email = "test@gmail.com";

        when(userQueryPort.existsByEmail(email)).thenReturn(true);

        String content = mockMvc.perform(get("/internal/users/exists")
                        .param("email", email)
                        .with(authentication(initAuth(null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExistsUserResponse existsUserResponse = objectMapper.readValue(content, ExistsUserResponse.class);
        assertThat(existsUserResponse).isNotNull();
        assertThat(existsUserResponse.exists()).isEqualTo(true);
    }

    @Test
    void update_shouldSuccessfullyUpdate() throws Exception {
        String id = String.valueOf(UUID.randomUUID());
        UpdateUserRequest request = UpdateUserRequestDataBuilder.withAllFields().build();

        doNothing().when(userCommandPort).update(id, request);

        mockMvc.perform(put("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .param("id", id)
                        .with(authentication(initAuth(null, null))))
                .andExpect(status().isOk());
    }

    @Test
    void update_shouldReturnNotFound() throws Exception {
        String id = String.valueOf(UUID.randomUUID());
        UpdateUserRequest request = UpdateUserRequestDataBuilder.withAllFields().build();

        String requestUri = "/internal/users";

        doThrow(new UserNotFoundException("User not found.")).when(userCommandPort).update(id, request);

        String content = mockMvc.perform(put(requestUri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .param("id", id)
                        .with(authentication(initAuth(null, null))))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse response = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("User not found.");
        assertThat(response.getPath()).isEqualTo(requestUri);
        assertThat(response.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void save_shouldSuccessfullyPersist() throws Exception {
        User user = UserDataBuilder.withAllFields().build();
        SaveUserRequest request = SaveUserRequestDataBuilder.withAllFields().build();

        when(userCommandPort.save(request)).thenReturn(user);

        String content = mockMvc.perform(post("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(initAuth(null, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();
        User userResponse = objectMapper.readValue(content, User.class);

        AssertionUtils.assertFromDto(userResponse, user);
    }

    @Test
    void save_shouldReturnNotFound() throws Exception {
        SaveUserRequest request = SaveUserRequestDataBuilder.withAllFields().build();
        String requestUri = "/internal/users";

        doThrow(new UserNotFoundException("User not found.")).when(userCommandPort).save(request);

        String content = mockMvc.perform(post(requestUri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(initAuth(null, null))))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse response = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("User not found.");
        assertThat(response.getPath()).isEqualTo(requestUri);
        assertThat(response.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}
