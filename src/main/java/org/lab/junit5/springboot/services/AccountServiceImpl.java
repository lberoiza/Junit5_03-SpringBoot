package org.lab.junit5.springboot.services;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.lab.junit5.springboot.exceptions.AccountNotFoundByIdException;
import org.lab.junit5.springboot.exceptions.AccountNotFoundByNumberException;
import org.lab.junit5.springboot.models.entitites.Account;
import org.lab.junit5.springboot.repositories.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepository;
  private final BankService bankService;

  @Override
  @Transactional(readOnly = true)
  public Account findAccountById(Long accountId) {
    return accountRepository
        .findById(accountId)
        .orElseThrow(() -> new AccountNotFoundByIdException(accountId));
  }

  @Override
  @Transactional(readOnly = true)
  public Account findAccountByAccountNumber(String accountNumber) {
    return accountRepository
        .findByAccountNumber(accountNumber)
        .orElseThrow(() -> new AccountNotFoundByNumberException(accountNumber));
  }

  @Override
  @Transactional
  public Account save(Account account) {
    return accountRepository.save(account);
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal getBalance(Long accountId) {
    return accountRepository
        .findById(accountId)
        .map(Account::getBalance)
        .orElseThrow(() -> new AccountNotFoundByIdException(accountId));
  }

  @Override
  @Transactional
  public void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount, Long bankId) {
    Account sourceAccount = findAccountById(sourceAccountId);
    Account targetAccount = findAccountById(targetAccountId);
    sourceAccount.withdraw(amount);
    save(sourceAccount);
    targetAccount.deposit(amount);
    save(targetAccount);
    bankService.updateTotalOfTransactions(bankId);
  }
}
