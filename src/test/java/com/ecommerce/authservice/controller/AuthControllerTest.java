package com.ecommerce.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.authservice.config.SecurityConfig;
import com.ecommerce.authservice.dto.LoginRequest;
import com.ecommerce.authservice.dto.RegisterRequest;
import com.ecommerce.authservice.dto.TokenResponse;
import com.ecommerce.authservice.service.AuthService;
import com.ecommerce.authservice.service.JwtService;
import io.jsonwebtoken.Jwts;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

  @Autowired private MockMvc mvc;

  @MockitoBean private AuthService authService;

  @MockitoBean private JwtService jwtService;

  @Test
  void register_returns201() throws Exception {
    mvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"secret\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void register_duplicateEmail_returns409() throws Exception {
    doThrow(new IllegalArgumentException("Email already registered"))
        .when(authService)
        .register(any(RegisterRequest.class));

    mvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"secret\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REGISTRATION_CONFLICT"));
  }

  @Test
  void login_returnsToken() throws Exception {
    when(authService.login(any(LoginRequest.class)))
        .thenReturn(TokenResponse.bearer("a.b.c", 3_600_000L));

    mvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"secret\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("a.b.c"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  void login_badCredentials_returns401() throws Exception {
    when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

    mvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void validate_echoesSubjectAndRoles() throws Exception {
    when(jwtService.parse("a.b.c"))
        .thenReturn(
            Jwts.claims().subject("ada@example.com").add("roles", List.of("customer")).build());

    mvc.perform(get("/auth/validate").header(HttpHeaders.AUTHORIZATION, "Bearer a.b.c"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subject").value("ada@example.com"));
  }
}
