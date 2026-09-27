package com.ecommerce.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.authservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {

  @Autowired private UserRepository repository;

  @BeforeEach
  void seed() {
    repository.save(
        User.builder().email("ada@example.com").password("hash").role("customer").build());
  }

  @Test
  void findByEmail_returnsMatch() {
    assertThat(repository.findByEmail("ada@example.com")).isPresent();
    assertThat(repository.findByEmail("nobody@example.com")).isEmpty();
  }

  @Test
  void existsByEmail_reflectsPersistence() {
    assertThat(repository.existsByEmail("ada@example.com")).isTrue();
    assertThat(repository.existsByEmail("nobody@example.com")).isFalse();
  }
}
