package org.lab.junit5.springboot.services;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.lab.junit5.springboot.exceptions.AccountNotFoundByIdException;
import org.lab.junit5.springboot.models.Account;
import org.lab.junit5.springboot.repositories.AccountRepository;

@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepository;
  private final BankService bankService;


  @Override
  public Account findAccountById(Long accountId) {
    return accountRepository
        .findById(accountId)
        .orElseThrow(() -> new AccountNotFoundByIdException(accountId));
  }

  @Override
  public Account createAccount(Account account) {
    return accountRepository.save(account);
  }

  @Override
  public int getTotalOfTransactions(Long bankId) {
    return bankService.findBankById(bankId).getTotalOfTransfers();
  }

  @Override
  public BigDecimal getBalance(Long accountId) {
    return accountRepository.findById(accountId)
        .map(Account::getBalance)
        .orElseThrow(() -> new AccountNotFoundByIdException(accountId));
  }

  @Override
  public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
    Account fromAccount = findAccountById(fromAccountId);
    Account toAccount = findAccountById(toAccountId);
    fromAccount.withdraw(amount);
    accountRepository.save(fromAccount);
    toAccount.deposit(amount);
    accountRepository.save(toAccount);
  }


}
