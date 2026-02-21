package com.vendo.user_service.adapter.in.security;

import com.vendo.core_lib.exception.ExceptionResponse;
import com.vendo.core_lib.type.ServiceName;
import com.vendo.core_lib.type.ServiceRole;
import com.vendo.security_lib.exception.InvalidTokenException;
import com.vendo.user_service.adapter.in.security.builder.InternalClaimPayloadDataBuilder;
import com.vendo.user_service.adapter.out.security.jwt.TokenClaimsParser;
import com.vendo.user_service.adapter.out.security.jwt.dto.InternalClaimPayload;
import com.vendo.user_service.adapter.out.security.util.SecurityContextUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static com.vendo.security_lib.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.vendo.security_lib.constants.AuthConstants.BEARER_PREFIX;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InternalGatewayFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TokenClaimsParser tokenClaimsParser;

    @Test
    void doFilterInternal_shouldSuccessfullyFilter() throws Exception {
        String token = "valid_token";
        InternalClaimPayload payload = InternalClaimPayloadDataBuilder.buildWithAllFields().build();

        when(tokenClaimsParser.parseInternalClaims(token)).thenReturn(payload);

        String content = mockMvc.perform(get("/internal/test/ping")
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();
        assertThat(content).isEqualTo("pong");
    }

    @Test
    void doFilterInternal_shouldSuccessfullyFilter_whenAlreadyAuthorized() throws Exception {
        String subject = ServiceName.AUTH_SERVICE.toString();
        GrantedAuthority authority = new SimpleGrantedAuthority(ServiceRole.INTERNAL.toString());

        Authentication auth = SecurityContextUtils.initializeAuthentication(subject, List.of(authority));
        String content = mockMvc.perform(get("/internal/test/ping").with(authentication(auth)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();
        assertThat(content).isEqualTo("pong");
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorized_whenNoToken() throws Exception {
        String requestPath = "/internal/test/ping";

        String content = mockMvc.perform(get(requestPath))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);
        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getMessage()).isEqualTo("Unauthorized.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getPath()).isEqualTo(requestPath);
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorized_whenTokenWithoutBearerPrefix() throws Exception {
        String requestPath = "/internal/test/ping";
        String token = "valid_token";

        String content = mockMvc.perform(get(requestPath).header(AUTHORIZATION_HEADER, token))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);
        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getMessage()).isEqualTo("Unauthorized.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getPath()).isEqualTo(requestPath);
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorized_whenNoTokenAfterBearerPrefix() throws Exception {
        String requestPath = "/internal/test/ping";

        String content = mockMvc.perform(get(requestPath).header(AUTHORIZATION_HEADER))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);
        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getMessage()).isEqualTo("Unauthorized.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getPath()).isEqualTo(requestPath);
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorized_whenInvalidToken() throws Exception {
        String requestPath = "/internal/test/ping";
        String invalidToken = "invalid_token";

        when(tokenClaimsParser.parseInternalClaims(invalidToken)).thenThrow(new InvalidTokenException("Invalid token."));

        String content = mockMvc.perform(get(requestPath).header(AUTHORIZATION_HEADER, BEARER_PREFIX + invalidToken))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);
        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getMessage()).isEqualTo("Invalid token.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getPath()).isEqualTo(requestPath);
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorized_whenAudienceClaimIsNotUserService() throws Exception {
        InternalClaimPayload payload = InternalClaimPayloadDataBuilder.buildWithAllFields()
                .audience(ServiceName.AUTH_SERVICE.toString())
                .build();
        String requestPath = "/internal/test/ping";
        String token = "valid_token";

        when(tokenClaimsParser.parseInternalClaims(token)).thenReturn(payload);

        String content = mockMvc.perform(get(requestPath).header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);
        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getMessage()).isEqualTo("Invalid token.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getPath()).isEqualTo(requestPath);
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorized_whenRoleIsNotInternal() throws Exception {
        InternalClaimPayload payload = InternalClaimPayloadDataBuilder.buildWithAllFields()
                .roles(List.of("NOT_INTERNAL"))
                .build();
        String requestPath = "/internal/test/ping";
        String token = "valid_token";

        when(tokenClaimsParser.parseInternalClaims(token)).thenReturn(payload);

        String content = mockMvc.perform(get(requestPath).header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);
        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getMessage()).isEqualTo("Invalid token.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getPath()).isEqualTo(requestPath);
    }

}
