package com.ecommerce.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.authservice.entity.Roles;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoAccountsTest {

  private static final String STAFF = "staff@example.com";
  private static final String ADMIN = "admin@example.com";
  private static final String GOOD_PASSWORD = "a-long-generated-password";

  private final UserRepository repository = mock(UserRepository.class);
  private final PasswordEncoder encoder = mock(PasswordEncoder.class);

  private DemoAccounts accounts(String staffPassword, String adminPassword) {
    when(encoder.encode(any())).thenAnswer(i -> "hashed:" + i.getArgument(0));
    return new DemoAccounts(repository, encoder, STAFF, staffPassword, ADMIN, adminPassword);
  }

  @Test
  void noPasswords_noAccounts() {
    accounts("", "").run(null);

    verify(repository, never()).save(any());
  }

  @Test
  void createsStaffAndAdmin_withTheirRoleAndAHashedPassword() {
    when(repository.findByEmail(any())).thenReturn(Optional.empty());

    accounts(GOOD_PASSWORD, GOOD_PASSWORD).run(null);

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(repository, times(2)).save(saved.capture());
    List<User> users = saved.getAllValues();
    assertThat(users)
        .extracting(User::getEmail, User::getRole)
        .containsExactly(tuple(STAFF, Roles.STAFF), tuple(ADMIN, Roles.ADMIN));
    assertThat(users)
        .allSatisfy(u -> assertThat(u.getPassword()).isEqualTo("hashed:" + GOOD_PASSWORD));
  }

  @Test
  void existingAccount_isResetToConfiguredRoleAndPassword() {
    // e.g. someone signed up with the admin email before the account was seeded
    User squatter = User.builder().email(ADMIN).password("theirs").role(Roles.CUSTOMER).build();
    when(repository.findByEmail(ADMIN)).thenReturn(Optional.of(squatter));

    accounts("", GOOD_PASSWORD).run(null);

    assertThat(squatter.getRole()).isEqualTo(Roles.ADMIN);
    assertThat(squatter.getPassword()).isEqualTo("hashed:" + GOOD_PASSWORD);
  }

  @Test
  void shortPassword_stopsStartup() {
    assertThatThrownBy(() -> accounts("short", "").run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least " + DemoAccounts.MIN_PASSWORD_LENGTH);
  }
}
