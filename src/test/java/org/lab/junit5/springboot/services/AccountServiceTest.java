package org.lab.junit5.springboot.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lab.junit5.springboot.models.Account;
import org.lab.junit5.springboot.models.Bank;
import org.lab.junit5.springboot.repositories.AccountRepository;
import org.lab.junit5.springboot.repositories.BankRepository;
import org.lab.junit5.springboot.testdata.AccountTestDataBuilder;
import org.lab.junit5.springboot.testdata.BankTestDataBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class AccountServiceTest {

  @MockBean private AccountRepository accountRepository;
  @MockBean private BankRepository bankRepository;

  private BankService bankService;
  private AccountService accountService;

  private Bank bank;
  private Account sourceAccount;
  private Account targetAccount;

  @BeforeEach
  void setUp() {
    bankService = new BankServiceImpl(bankRepository);
    accountService = new AccountServiceImpl(accountRepository, bankService);

    bank = BankTestDataBuilder.random().build();

    sourceAccount = AccountTestDataBuilder.random().build();
    targetAccount = AccountTestDataBuilder.random().build();
  }

  @Test
  void test_transfer_money_then_ok() {
    // Create Test Data
    int initialTotalOfTransactions = bank.getTotalOfTransactions();

    // Prepare Mocks
    prepareMocksForTransfer();

    // Get initial balances and check them
    BigDecimal initialSourceAccountBalance = accountService.getBalance(sourceAccount.getId());
    assertThat(sourceAccount.getBalance()).isEqualTo(initialSourceAccountBalance);
    BigDecimal initialTargetAccountBalance = accountService.getBalance(targetAccount.getId());
    assertThat(targetAccount.getBalance()).isEqualTo(initialTargetAccountBalance);

    // Transfer Money
    accountService.transfer(
        sourceAccount.getId(), targetAccount.getId(), initialSourceAccountBalance, bank.getId());

    // Check Results
    // Account 1 has 0 balance
    assertBalanceAfterTransaction(sourceAccount.getId(), BigDecimal.ZERO);

    // Account 2 has the sum of the initial balances
    BigDecimal expectedTargetAccountBalance =
        initialTargetAccountBalance.add(initialSourceAccountBalance);
    assertBalanceAfterTransaction(targetAccount.getId(), expectedTargetAccountBalance);

    // Bank has one more transfer
    assertBankTotalOfTransactionsAfterTransaction(initialTotalOfTransactions + 1);
  }

  private void prepareMocksForTransfer() {
    when(bankRepository.findById(bank.getId())).thenReturn(Optional.of(bank));
    when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
    when(accountRepository.findById(targetAccount.getId())).thenReturn(Optional.of(targetAccount));
  }

  private void assertBalanceAfterTransaction(Long accountId, BigDecimal expectedBalance) {
    BigDecimal updatedSourceAccountBalance =
        accountService.getBalance(accountId).setScale(2, RoundingMode.HALF_UP);
    expectedBalance = expectedBalance.setScale(2, RoundingMode.HALF_UP);
    assertThat(updatedSourceAccountBalance).isEqualTo(expectedBalance);
  }

  private void assertBankTotalOfTransactionsAfterTransaction(int expectedTotalOfTransactions) {
    int totalOfTransactions = bankService.getTotalOfTransactions(bank.getId());
    assertThat(totalOfTransactions).isEqualTo(expectedTotalOfTransactions);
  }
}
