package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dto.LoginRequest;
import com.ecommerce.authservice.dto.RegisterRequest;
import com.ecommerce.authservice.dto.TokenResponse;
import com.ecommerce.authservice.entity.Roles;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.repository.UserRepository;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and login. */
@Service
public class AuthService {

  private static final String DEFAULT_ROLE = Roles.CUSTOMER;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  /**
   * Registers a new user with a BCrypt-hashed password.
   *
   * @throws IllegalArgumentException if the email is already registered
   */
  @Transactional
  public void register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("Email already registered");
    }
    User user =
        User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role(DEFAULT_ROLE)
            .build();
    userRepository.save(user);
  }

  /**
   * Verifies credentials and returns a signed token.
   *
   * @throws BadCredentialsException if the email is unknown or the password does not match
   */
  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    String token = jwtService.generateToken(user.getEmail(), List.of(user.getRole()));
    return TokenResponse.bearer(token, jwtService.getExpirationMs());
  }
}
