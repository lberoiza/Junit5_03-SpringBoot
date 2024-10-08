package org.lab.junit5.springboot.services;

import org.lab.junit5.springboot.models.Account;

import java.math.BigDecimal;

public interface AccountService {

  Account findAccountById(Long accountId);
  Account save(Account account);
  int getTotalOfTransactions(Long bankId);
  BigDecimal getBalance(Long accountId);
  void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, Long bankId);
}
