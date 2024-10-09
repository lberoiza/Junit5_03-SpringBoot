package org.lab.junit5.springboot.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
import org.mockito.InOrder;
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
    // Prepare Mocks
    prepareMocksForTransfer();

    // get initial total of transactions - bank findById
    int initialTotalOfTransactions = bankService.getTotalOfTransactions(bank.getId());

    // Get initial balances and check them -  Account findById (1 time for source and 1 time for
    // target)
    BigDecimal initialSourceAccountBalance = accountService.getBalance(sourceAccount.getId());
    assertThat(sourceAccount.getBalance()).isEqualTo(initialSourceAccountBalance);
    BigDecimal initialTargetAccountBalance = accountService.getBalance(targetAccount.getId());
    assertThat(targetAccount.getBalance()).isEqualTo(initialTargetAccountBalance);

    // Transfer Money - Account findById (1 time for source, 1 time for target and 1 time for bank)
    accountService.transfer(
        sourceAccount.getId(), targetAccount.getId(), initialSourceAccountBalance, bank.getId());

    // Check Results
    // Account 1 has 0 balance - account findById (1 time for source)
    assertBalanceAfterTransaction(sourceAccount.getId(), BigDecimal.ZERO);

    // Account 2 has the sum of the initial balances - account findById (1 time for target)
    BigDecimal expectedTargetAccountBalance =
        initialTargetAccountBalance.add(initialSourceAccountBalance);
    assertBalanceAfterTransaction(targetAccount.getId(), expectedTargetAccountBalance);

    // Bank has one more transfer - bank findById (1 time for bank)
    assertBankTotalOfTransactionsAfterTransaction(initialTotalOfTransactions + 1);

    // Verify Mocks total of executions
    verifyMocksAfterTransaction();

    // Verify Order of Mock executions
    verifyOrderOfMocksExecution();
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

  private void verifyMocksAfterTransaction() {
    verify(bankRepository, times(3)).findById(bank.getId());
    verify(accountRepository, times(3)).findById(sourceAccount.getId());
    verify(accountRepository, times(3)).findById(targetAccount.getId());

    verify(accountRepository, times(1)).save(sourceAccount);
    verify(accountRepository, times(1)).save(targetAccount);
  }

  private void verifyOrderOfMocksExecution() {
    InOrder inOrder = inOrder(bankRepository, accountRepository);

    // get initial values of transactions and balances
    inOrder.verify(bankRepository).findById(bank.getId());
    inOrder.verify(accountRepository).findById(sourceAccount.getId());
    inOrder.verify(accountRepository).findById(targetAccount.getId());

    // transfer money
    inOrder.verify(accountRepository).findById(sourceAccount.getId());
    inOrder.verify(accountRepository).findById(targetAccount.getId());
    inOrder.verify(accountRepository).save(sourceAccount);
    inOrder.verify(accountRepository).save(targetAccount);
    inOrder.verify(bankRepository).save(bank);

    // assert of final transactions and balances
    inOrder.verify(accountRepository).findById(sourceAccount.getId());
    inOrder.verify(accountRepository).findById(targetAccount.getId());
    inOrder.verify(bankRepository).findById(bank.getId());
  }
}
