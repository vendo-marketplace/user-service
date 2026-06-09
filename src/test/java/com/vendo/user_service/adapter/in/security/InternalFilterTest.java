package com.vendo.user_service.adapter.in.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendo.core_lib.type.ServiceName;
import com.vendo.core_lib.type.ServiceRole;
import com.vendo.security_lib.exception.response.ExceptionResponse;
import com.vendo.security_starter.jwt.parser.TokenClaims;
import com.vendo.security_starter.jwt.parser.TokenClaimsParser;
import com.vendo.user_service.adapter.in.security.builder.TokenClaimsDataBuilder;
import com.vendo.user_service.adapter.out.security.util.SecurityContextUtils;
import com.vendo.user_service.adapter.security.out.props.JwtProperties;
import com.vendo.user_service.test_utils.controller.PongRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static com.vendo.security_lib.http.HttpUtils.AUTHORIZATION_HEADER;
import static com.vendo.security_lib.http.HttpUtils.BEARER_PREFIX;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InternalFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties props;

    @MockitoBean
    private TokenClaimsParser tokenClaimsParser;

    @Test
    void doFilterInternal_shouldSuccessfullyFilter() throws Exception {
        String token = "valid_token";
        TokenClaims payload = TokenClaimsDataBuilder.buildWithAllFields().build();

        when(tokenClaimsParser.extract(token, props.getInternal().key())).thenReturn(payload);

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

        Authentication auth = SecurityContextUtils.initAuth(subject, List.of(authority));
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
    void doFilterInternal_shouldReturnUnsupportedMediaType_whenMediaIsText() throws Exception {
        String requestPath = "/internal/ping/pong";
        PongRequest request = new PongRequest("content");
        String subject = ServiceName.AUTH_SERVICE.toString();
        GrantedAuthority authority = new SimpleGrantedAuthority(ServiceRole.INTERNAL.toString());

        Authentication auth = SecurityContextUtils.initAuth(subject, List.of(authority));
        String content = mockMvc.perform(post(requestPath)
                        .with(authentication(auth))
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isUnsupportedMediaType())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);
        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getMessage()).isEqualTo("Unsupported media type.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
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
    void doFilterInternal_shouldReturnUnauthorized_whenInvalidToken() throws Exception {
        String requestPath = "/internal/test/ping";
        String invalidToken = "invalid_token";

        when(tokenClaimsParser.extract(invalidToken, props.getInternal().key())).thenThrow(new BadCredentialsException("Unauthorized."));

        String content = mockMvc.perform(get(requestPath).header(AUTHORIZATION_HEADER, BEARER_PREFIX + invalidToken))
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
    void doFilterInternal_shouldReturnUnauthorized_whenAudienceClaimIsNotUserService() throws Exception {
        TokenClaims payload = TokenClaimsDataBuilder.buildWithAllFields()
                .audience(Set.of(ServiceName.AUTH_SERVICE.toString()))
                .build();
        String requestPath = "/internal/test/ping";
        String token = "valid_token";

        when(tokenClaimsParser.extract(token, props.getInternal().key())).thenReturn(payload);

        String content = mockMvc.perform(get(requestPath).header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
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
    void doFilterInternal_shouldReturnUnauthorized_whenRoleIsNotInternal() throws Exception {
        TokenClaims payload = TokenClaimsDataBuilder.buildWithAllFields()
                .roles(Set.of("NOT_INTERNAL"))
                .build();
        String requestPath = "/internal/test/ping";
        String token = "valid_token";

        when(tokenClaimsParser.extract(token, props.getInternal().key())).thenReturn(payload);

        String content = mockMvc.perform(get(requestPath).header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(content).isNotBlank();

        ExceptionResponse exceptionResponse = objectMapper.readValue(content, ExceptionResponse.class);
        assertThat(exceptionResponse).isNotNull();
        assertThat(exceptionResponse.getMessage()).isEqualTo("Unauthorized.");
        assertThat(exceptionResponse.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(exceptionResponse.getPath()).isEqualTo(requestPath);
    }

}
