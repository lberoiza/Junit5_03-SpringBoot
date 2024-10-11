package org.lab.junit5.springboot.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lab.junit5.springboot.models.Account;
import org.lab.junit5.springboot.testdata.AccountTestDataBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class AccountRepositoryTest {

  @Autowired private AccountRepository accountRepository;

  private Account savedAccount;

  @BeforeEach
  void setUp() {
    savedAccount = AccountTestDataBuilder.random().withId(null).build();
    savedAccount = accountRepository.save(savedAccount);
  }

  @Test
  void test_save() {
    assertThat(savedAccount.getId()).isNotNull();
  }

  @Test
  void test_findById_then_found_account() {
    Optional<Account> foundAccount = accountRepository.findById(savedAccount.getId());
    assertThat(foundAccount).isPresent();
    assertThat(foundAccount.get()).isEqualTo(savedAccount);
  }

  @Test
  void test_findByAccountNumber_then_found_account() {
    Optional<Account> foundAccount =
        accountRepository.findByAccountNumber(savedAccount.getAccountNumber());
    assertThat(foundAccount).isPresent();
    assertThat(foundAccount.get()).isEqualTo(savedAccount);
  }

  @Test
  void test_findById_then_optional_empty() {
    Optional<Account> foundAccount = accountRepository.findById(savedAccount.getId() * 2);
    assertThat(foundAccount).isEmpty();
  }

  @Test
  void test_findByAccountNumber_then_optional_empty() {
    Optional<Account> foundAccount =
        accountRepository.findByAccountNumber(savedAccount.getAccountNumber() + "TEST");
    assertThat(foundAccount).isEmpty();
  }

  @Test
  void test_findAll_then_found_accounts() {
    List<Account> savedAccounts = addMoreAccounts();
    List<Account> allAccountsFromDB = accountRepository.findAll();
    savedAccounts.add(savedAccount);
    assertThat(allAccountsFromDB).containsExactlyInAnyOrderElementsOf(savedAccounts);
  }

  private List<Account> addMoreAccounts() {
    List<Account> accounts =
        List.of(
            AccountTestDataBuilder.random().withId(null).build(),
            AccountTestDataBuilder.random().withId(null).build(),
            AccountTestDataBuilder.random().withId(null).build());

    return accountRepository.saveAll(accounts);
  }
}
