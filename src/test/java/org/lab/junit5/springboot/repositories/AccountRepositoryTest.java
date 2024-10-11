package org.lab.junit5.springboot.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lab.junit5.springboot.models.Account;
import org.lab.junit5.springboot.testdata.AccountTestDataBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

  @Autowired private AccountRepository accountRepository;

  private AccountTestDataBuilder accountTestDataBuilder;
  private Account savedAccount;

  @BeforeEach
  void setUp() {
    accountTestDataBuilder = AccountTestDataBuilder.random().withId(null);
    savedAccount = accountTestDataBuilder.build();
    savedAccount = accountRepository.save(savedAccount);
  }

  @Test
  void test_save() {
    assertThat(savedAccount.getId()).isNotNull();
  }

  @Test
  void test_findById() {
    Optional<Account> foundAccount = accountRepository.findById(savedAccount.getId());
    assertThat(foundAccount).isPresent();
    assertThat(foundAccount.get()).isEqualTo(savedAccount);
  }

  @Test
  void test_findByAccountNumber() {
    Optional<Account> foundAccount =
        accountRepository.findByAccountNumber(savedAccount.getAccountNumber());
    assertThat(foundAccount).isPresent();
    assertThat(foundAccount.get()).isEqualTo(savedAccount);
  }
}
