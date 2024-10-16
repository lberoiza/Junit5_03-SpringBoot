package org.lab.junit5.springboot.services;

import org.lab.junit5.springboot.models.entitites.Account;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

  List<Account> findAllAccounts();

  Account findAccountById(Long accountId);

  Account findAccountByAccountNumber(String accountNumber);

  Account save(Account account);

  BigDecimal getBalance(Long accountId);

  void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, Long bankId);
}
