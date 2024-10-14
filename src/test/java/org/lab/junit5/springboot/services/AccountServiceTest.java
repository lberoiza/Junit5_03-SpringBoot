package org.lab.junit5.springboot.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lab.junit5.springboot.exceptions.AccountException;
import org.lab.junit5.springboot.exceptions.AccountInsufficientMoneyException;
import org.lab.junit5.springboot.models.entitites.Account;
import org.lab.junit5.springboot.models.entitites.Bank;
import org.lab.junit5.springboot.repositories.AccountRepository;
import org.lab.junit5.springboot.repositories.BankRepository;
import org.lab.junit5.springboot.testdata.AccountTestDataBuilder;
import org.lab.junit5.springboot.testdata.BankTestDataBuilder;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class AccountServiceTest {

  @MockBean private AccountRepository accountRepository;
  @MockBean private BankRepository bankRepository;

  @Autowired private BankService bankService;
  @Autowired private AccountService accountService;

  private Bank bank;
  private Account sourceAccount;
  private Account targetAccount;

  @BeforeEach
  void setUp() {
    bank = BankTestDataBuilder.random().build();

    sourceAccount = AccountTestDataBuilder.random().build();
    targetAccount = AccountTestDataBuilder.random().build();
  }

  @Test
  void test_transfer_source_account_has_not_enough_money_then_exception() {
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
    assertThatExceptionOfType(AccountException.class)
        .isThrownBy(
            () ->
                accountService.transfer(
                    sourceAccount.getId(),
                    targetAccount.getId(),
                    initialSourceAccountBalance.multiply(BigDecimal.TWO),
                    bank.getId()))
        .isExactlyInstanceOf(AccountInsufficientMoneyException.class)
        .withMessageContaining(
            getExceptionMessage(
                sourceAccount, initialSourceAccountBalance.multiply(BigDecimal.TWO)));

    // Check Results
    // Account 1 has 0 balance - account findById (1 time for source)
    assertBalanceAfterTransaction(sourceAccount.getId(), initialSourceAccountBalance);

    // Account 2 has the sum of the initial balances - account findById (1 time for target)
    assertBalanceAfterTransaction(targetAccount.getId(), initialTargetAccountBalance);

    // Bank has one more transfer - bank findById (1 time for bank)
    assertBankTotalOfTransactionsAfterTransaction(initialTotalOfTransactions);

    // Verify Mocks total of executions
    verifyMocksAfterFailedTransfer();

    // Verify Order of Mock executions
    verifyOrderOfMocksExecutionAfterFailedTransfer();
  }

  private String getExceptionMessage(Account account, BigDecimal amount) {
    return "Insufficient money in account nr: %s. Current balance: %.2f, requested amount: %.2f"
        .formatted(account.getAccountNumber(), account.getBalance(), amount);
  }

  private void verifyMocksAfterFailedTransfer() {
    verify(bankRepository, times(2)).findById(bank.getId());
    verify(accountRepository, times(3)).findById(sourceAccount.getId());
    verify(accountRepository, times(3)).findById(targetAccount.getId());

    verify(accountRepository, never()).save(sourceAccount);
    verify(accountRepository, never()).save(targetAccount);
  }

  private void verifyOrderOfMocksExecutionAfterFailedTransfer() {
    InOrder inOrder = inOrder(bankRepository, accountRepository);

    // get initial values of transactions and balances
    inOrder.verify(bankRepository).findById(bank.getId());
    inOrder.verify(accountRepository).findById(sourceAccount.getId());
    inOrder.verify(accountRepository).findById(targetAccount.getId());

    // transfer money
    inOrder.verify(accountRepository).findById(sourceAccount.getId());
    inOrder.verify(accountRepository).findById(targetAccount.getId());

    // assert of final transactions and balances
    inOrder.verify(accountRepository).findById(sourceAccount.getId());
    inOrder.verify(accountRepository).findById(targetAccount.getId());
    inOrder.verify(bankRepository).findById(bank.getId());
  }

  @Test
  void test_transfer_source_account_has_enough_money_then_ok() {
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
    // Transfer Money - Account save (1 time for source, 1 time for target and 1 time for bank)
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
    verifyMocksAfterSuccessfullyTransfer();

    // Verify Order of Mock executions
    verifyOrderOfMocksExecutionAfterSuccessfullyTransfer();
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

  private void verifyMocksAfterSuccessfullyTransfer() {
    verify(bankRepository, times(3)).findById(bank.getId());
    verify(accountRepository, times(3)).findById(sourceAccount.getId());
    verify(accountRepository, times(3)).findById(targetAccount.getId());

    verify(accountRepository, times(1)).save(sourceAccount);
    verify(accountRepository, times(1)).save(targetAccount);
  }

  private void verifyOrderOfMocksExecutionAfterSuccessfullyTransfer() {
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
    inOrder.verify(bankRepository).findById(bank.getId());
    inOrder.verify(bankRepository).save(bank);

    // assert of final transactions and balances
    inOrder.verify(accountRepository).findById(sourceAccount.getId());
    inOrder.verify(accountRepository).findById(targetAccount.getId());
    inOrder.verify(bankRepository).findById(bank.getId());
  }

  @Test
  void save_account() {
    Long generatedId = 1L;
    Account account = AccountTestDataBuilder.random().withId(null).build();

    // Cuando modificas el objeto que se pasa como argumento dentro de la simulación de la
    // llamada (en tu caso, dentro de doAnswer o then), Mockito cree que el argumento que se pasó a
    // la función es el mismo que luego fue modificado. Por lo tanto, al hacer una verificación con
    // verify(), esperará que el objeto pasado a la función sea el que tiene los valores
    // modificados, no el original.
    // Por ejemplo, si llamas a save(account) y luego modificas el account dentro de la
    // simulación (como asignarle un ID), entonces Mockito pensará que el argumento pasado fue el
    // objeto modificado, y al usar verify(), esperará que le pases el objeto modificado, no el
    // original.
    when(accountRepository.save(any(Account.class)))
        .then(
            invocation -> {
              Account accountToSave = invocation.getArgument(0);
              return cloneAccount(accountToSave).setId(generatedId);
            });

    Account savedAccount = accountService.save(account);

    assertThat(savedAccount).isNotNull();
    assertThat(savedAccount.getId()).isEqualTo(generatedId);

    verify(accountRepository).save(account);
  }

  private Account cloneAccount(Account account) {
    return new Account()
        .setId(account.getId())
        .setAccountNumber(account.getAccountNumber())
        .setOwner(account.getOwner())
        .setBalance(account.getBalance());
  }
}
