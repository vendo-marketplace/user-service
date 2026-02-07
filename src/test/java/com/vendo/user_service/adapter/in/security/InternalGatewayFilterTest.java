package com.vendo.user_service.adapter.in.security;

import com.vendo.common.exception.ExceptionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@WebMvcTest(TestInternalController.class)
@Import(InternalAntPathResolver.class)
public class InternalGatewayFilterTest {

    // TODO move to common
    private static final String INTERNAL_HEADER = "X-Internal-Api-Key";

    @Value("${internal.api-key}")
    private String INTERNAL_API_KEY;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void doFilterInternal_shouldSuccessfullyFilter() throws Exception {
        String content = mockMvc.perform(get("/internal/test/ping").header(INTERNAL_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();
        assertThat(content).isEqualTo("pong");
    }

    @Test
    void doFilterInternal_shouldReturnForbidden_whenApiKeyIsNull() throws Exception {
        String requestUri = "/internal/test/ping";

        String content = mockMvc.perform(get(requestUri))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(exceptionResponse.getPath()).isEqualTo(requestUri);
        assertThat(exceptionResponse.getMessage()).isEqualTo("Invalid credentials.");
    }

    @Test
    void doFilterInternal_shouldReturnForbidden_whenApiKeyIsInvalid() throws Exception {
        String requestUri = "/internal/test/ping";
        String invalidApiKey = "invalid_api_key";

        String content = mockMvc.perform(get(requestUri).header(INTERNAL_HEADER, invalidApiKey))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);

        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(exceptionResponse.getPath()).isEqualTo(requestUri);
        assertThat(exceptionResponse.getMessage()).isEqualTo("Invalid credentials.");
    }
}
