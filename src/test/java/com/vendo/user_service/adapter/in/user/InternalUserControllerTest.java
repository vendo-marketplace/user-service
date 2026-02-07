package com.vendo.user_service.adapter.in.user;

import com.vendo.common.exception.ExceptionResponse;
import com.vendo.user_service.adapter.in.security.InternalAntPathResolver;
import com.vendo.user_service.application.InternalUserService;
import com.vendo.user_service.domain.user.User;
import com.vendo.user_service.domain.user.UserDataBuilder;
import com.vendo.user_service.domain.user.dto.ExistsUserResponse;
import com.vendo.user_service.domain.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

         assertThat(userResponse).isNotNull();
         assertThat(userResponse.getEmail()).isEqualTo(user.getEmail());
         assertThat(userResponse.getPassword()).isEqualTo(user.getPassword());
         assertThat(userResponse.getEmailVerified()).isEqualTo(user.getEmailVerified());
         assertThat(userResponse.getFullName()).isEqualTo(user.getFullName());
         assertThat(userResponse.getBirthDate()).isEqualTo(user.getBirthDate());
         assertThat(userResponse.getProviderType()).isEqualTo(user.getProviderType());
         assertThat(userResponse.getStatus()).isEqualTo(user.getStatus());
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
}
