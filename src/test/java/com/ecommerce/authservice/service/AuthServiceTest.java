package com.ecommerce.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.authservice.dto.LoginRequest;
import com.ecommerce.authservice.dto.RegisterRequest;
import com.ecommerce.authservice.entity.Roles;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

  private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

  private UserRepository repository;
  private AuthService authService;
  private PasswordEncoder encoder;

  @BeforeEach
  void setUp() {
    repository = Mockito.mock(UserRepository.class);
    encoder = new BCryptPasswordEncoder(4);
    JwtService jwtService = new JwtService(SECRET, 3_600_000L, "ecommerce-auth-test");
    authService = new AuthService(repository, encoder, jwtService);
  }

  @Test
  void register_alwaysGivesTheCustomerRole() {
    authService.register(new RegisterRequest("ada@example.com", "pw"));

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    Mockito.verify(repository).save(saved.capture());
    assertThat(saved.getValue().getRole()).isEqualTo(Roles.CUSTOMER);
  }

  @Test
  void register_rejectsDuplicateEmail() {
    Mockito.when(repository.existsByEmail("ada@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(new RegisterRequest("ada@example.com", "pw")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void login_returnsTokenForValidCredentials() {
    User user =
        User.builder()
            .email("ada@example.com")
            .password(encoder.encode("secret"))
            .role("customer")
            .build();
    Mockito.when(repository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

    var response = authService.login(new LoginRequest("ada@example.com", "secret"));

    assertThat(response.token()).isNotBlank();
    assertThat(response.tokenType()).isEqualTo("Bearer");
  }

  @Test
  void login_rejectsWrongPassword() {
    User user =
        User.builder()
            .email("ada@example.com")
            .password(encoder.encode("secret"))
            .role("customer")
            .build();
    Mockito.when(repository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "wrong")))
        .isInstanceOf(BadCredentialsException.class);
  }
}
