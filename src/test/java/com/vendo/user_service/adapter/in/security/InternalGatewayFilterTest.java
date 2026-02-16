package com.vendo.user_service.adapter.in.security;

import com.vendo.user_service.adapter.out.security.jwt.TokenValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@WebMvcTest(InternalGatewayFilter.class)
public class InternalGatewayFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TokenValidationService tokenValidationService;

    @Test
    void doFilterInternal_shouldSuccessfullyFilter() throws Exception {
//        when(tokenValidationService.validate(apiKey)).thenReturn(true);
//
//        String content = mockMvc.perform(get("/internal/test/ping").with(authentication()))
//                .andExpect(status().isOk())
//                .andReturn().getResponse().getContentAsString();
//
//        assertThat(content).isNotBlank();
//        assertThat(content).isEqualTo("pong");
    }

}
