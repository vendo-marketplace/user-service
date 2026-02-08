package com.vendo.user_service.adapter.in.user;

import com.vendo.common.exception.ExceptionResponse;
import com.vendo.user_service.adapter.AssertionUtils;
import com.vendo.user_service.adapter.in.security.InternalAntPathResolver;
import com.vendo.user_service.application.InternalUserService;
import com.vendo.user_service.domain.user.SaveUserRequestDataBuilder;
import com.vendo.user_service.domain.user.UpdateUserRequestDataBuilder;
import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.domain.user.UserDataBuilder;
import com.vendo.user_service.domain.user.dto.ExistsUserResponse;
import com.vendo.user_service.domain.user.dto.SaveUserRequest;
import com.vendo.user_service.domain.user.dto.UpdateUserRequest;
import com.vendo.user_service.domain.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@WebMvcTest(InternalUserController.class)
@Import(InternalAntPathResolver.class)
public class InternalUserControllerTest {

    // TODO move to common
    private static final String INTERNAL_HEADER = "X-Internal-Api-Key";

    @Value("${internal.api-key}")
    private String INTERNAL_API_KEY;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InternalUserService internalUserService;

    @Test
    void getByEmail_shouldReturnUser() throws Exception {
        User user = UserDataBuilder.withAllFields().build();

        when(internalUserService.getByEmail(user.getEmail())).thenReturn(user);

        String content = mockMvc.perform(get("/internal/users")
                        .param("email", user.getEmail())
                        .header(INTERNAL_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();
        User userResponse = objectMapper.readValue(content, User.class);

        AssertionUtils.assertFromDto(user, userResponse);
    }

    @Test
    void getByEmail_shouldReturnNotFound() throws Exception {
        User user = UserDataBuilder.withAllFields().build();

        when(internalUserService.getByEmail(user.getEmail())).thenThrow(new UserNotFoundException("User not found."));

        String content = mockMvc.perform(get("/internal/users")
                        .param("email", user.getEmail())
                        .header(INTERNAL_HEADER, INTERNAL_API_KEY))
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
        ExistsUserResponse existsUserResponseMock = ExistsUserResponse.builder()
                .exists(true)
                .build();

        when(internalUserService.existsByEmail(email)).thenReturn(existsUserResponseMock);

        String content = mockMvc.perform(get("/internal/users/exists")
                        .param("email", email)
                        .header(INTERNAL_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExistsUserResponse existsUserResponse = objectMapper.readValue(content, ExistsUserResponse.class);
        assertThat(existsUserResponse).isNotNull();
        assertThat(existsUserResponse.exists()).isEqualTo(existsUserResponseMock.exists());
    }

    @Test
    void update_shouldSuccessfullyUpdate() throws Exception {
        String id = String.valueOf(UUID.randomUUID());
        UpdateUserRequest request = UpdateUserRequestDataBuilder.withAllFields().build();

        doNothing().when(internalUserService).update(id, request);

        mockMvc.perform(put("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .param("id", id)
                        .header(INTERNAL_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isOk());
    }

    @Test
    void update_shouldReturnNotFound() throws Exception {
        String id = String.valueOf(UUID.randomUUID());
        UpdateUserRequest request = UpdateUserRequestDataBuilder.withAllFields().build();
        String requestUri = "/internal/users";

        doThrow(new UserNotFoundException("User not found.")).when(internalUserService).update(id, request);

        String content = mockMvc.perform(put(requestUri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .param("id", id)
                        .header(INTERNAL_HEADER, INTERNAL_API_KEY))
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

        when(internalUserService.save(request)).thenReturn(user);

        String content = mockMvc.perform(post("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(INTERNAL_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();
        User userResponse = objectMapper.readValue(content, User.class);

        AssertionUtils.assertFromDto(userResponse, request);
    }

    @Test
    void save_shouldReturnNotFound() throws Exception {
        SaveUserRequest request = SaveUserRequestDataBuilder.withAllFields().build();
        String requestUri = "/internal/users";

        doThrow(new UserNotFoundException("User not found.")).when(internalUserService).save(request);

        String content = mockMvc.perform(post(requestUri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(INTERNAL_HEADER, INTERNAL_API_KEY))
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
