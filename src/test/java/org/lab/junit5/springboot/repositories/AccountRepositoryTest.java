package org.lab.junit5.springboot.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lab.junit5.springboot.models.entitites.Account;
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

  @Test
  void test_update_then_ok() {
    Long accountId = savedAccount.getId();
    BigDecimal newBalance = BigDecimal.valueOf(1000);
    savedAccount.setBalance(newBalance);

    Account updatedAccount = accountRepository.save(savedAccount);

    assertThat(updatedAccount.getId()).isEqualTo(accountId);
    assertThat(updatedAccount.getBalance()).isEqualTo(newBalance);
  }

  @Test
  void test_deleteById_then_ok() {
    Long accountId = savedAccount.getId();
    accountRepository.deleteById(accountId);

    assertThat(accountRepository.count()).isEqualTo(0);
    assertThat(accountRepository.findById(accountId)).isEmpty();
  }

  @Test
  void test_delete_then_ok() {
    accountRepository.delete(savedAccount);

    assertThat(accountRepository.count()).isEqualTo(0);
    assertThat(accountRepository.findById(savedAccount.getId())).isEmpty();
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
