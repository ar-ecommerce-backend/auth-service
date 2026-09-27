package com.ecommerce.authservice.service;

import com.ecommerce.authservice.entity.Roles;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the demo staff and admin accounts at startup, so the higher roles can be shown and tested
 * (docs ADR 0007). Each account exists only if its password is configured; the passwords are
 * generated per environment and injected as secrets, never committed. Config is the source of
 * truth: an existing account gets its role and password reset to match.
 */
@Component
public class DemoAccounts implements ApplicationRunner {

  static final int MIN_PASSWORD_LENGTH = 16;

  private static final Logger log = LoggerFactory.getLogger(DemoAccounts.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String staffEmail;
  private final String staffPassword;
  private final String adminEmail;
  private final String adminPassword;

  public DemoAccounts(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @Value("${demo-accounts.staff.email}") String staffEmail,
      @Value("${demo-accounts.staff.password:}") String staffPassword,
      @Value("${demo-accounts.admin.email}") String adminEmail,
      @Value("${demo-accounts.admin.password:}") String adminPassword) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.staffEmail = staffEmail;
    this.staffPassword = staffPassword;
    this.adminEmail = adminEmail;
    this.adminPassword = adminPassword;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    ensure(staffEmail, staffPassword, Roles.STAFF);
    ensure(adminEmail, adminPassword, Roles.ADMIN);
  }

  private void ensure(String email, String password, String role) {
    if (password == null || password.isBlank()) {
      return;
    }
    if (password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalStateException(
          "Demo " + role + " password must be at least " + MIN_PASSWORD_LENGTH + " characters");
    }
    User user =
        userRepository.findByEmail(email).orElseGet(() -> User.builder().email(email).build());
    user.setRole(role);
    user.setPassword(passwordEncoder.encode(password));
    userRepository.save(user);
    log.info("Demo account ready: {} ({})", email, role);
  }
}
