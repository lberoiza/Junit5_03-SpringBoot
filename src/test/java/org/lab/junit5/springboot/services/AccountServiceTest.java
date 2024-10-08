package org.lab.junit5.springboot.services;


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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
class AccountServiceTest {
  
  @MockBean private AccountRepository accountRepository;
  @MockBean private BankRepository bankRepository;

  private BankService bankService;
  private AccountService accountService;

  @BeforeEach
  void setUp() {
    bankService = new BankServiceImpl(bankRepository);
    accountService = new AccountServiceImpl(accountRepository, bankService);
  }


  @Test
  void test_transfer_money_then_ok() {

    // Create Test Data
    Bank bank = BankTestDataBuilder.random().build();
    int initialTotalOfTransfers = bank.getTotalOfTransfers();

    Account sourceAccount = AccountTestDataBuilder.random().build();
    Account targetAccount = AccountTestDataBuilder.random().build();

    // Prepare Mocks
    when(bankRepository.findById(bank.getId())).thenReturn(Optional.of(bank));
    when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
    when(accountRepository.findById(targetAccount.getId())).thenReturn(Optional.of(targetAccount));

    // Get Accounts and initial balances
    Account foundSourceAccount = accountService.findAccountById(sourceAccount.getId());
    assertThat(foundSourceAccount).isEqualTo(sourceAccount);
    BigDecimal initialSourceAccountBalance = foundSourceAccount.getBalance();

    Account foundTargetAccount = accountService.findAccountById(targetAccount.getId());
    assertThat(foundTargetAccount).isEqualTo(targetAccount);
    BigDecimal initialTargetAccountBalance = foundTargetAccount.getBalance();

    // Transfer Money
    accountService.transfer(sourceAccount.getId(), targetAccount.getId(), initialSourceAccountBalance, bank.getId());

    // Check Results
    // Account 1 has 0 balance
    Account updatedSourceAccount = accountService.findAccountById(sourceAccount.getId());
    assertThat(updatedSourceAccount.getBalance().setScale(2, RoundingMode.HALF_UP)).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

    // Account 2 has the sum of the initial balances
    Account updatedTargetAccount = accountService.findAccountById(targetAccount.getId());
    assertThat(updatedTargetAccount.getBalance()).isEqualTo(initialTargetAccountBalance.add(initialSourceAccountBalance));

    // Bank has one more transfer
    Bank updatedBank = bankService.findBankById(bank.getId());
    assertThat(updatedBank.getTotalOfTransfers()).isEqualTo(initialTotalOfTransfers + 1);
  }
}
