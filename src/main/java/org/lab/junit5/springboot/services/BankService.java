package org.lab.junit5.springboot.services;

import org.lab.junit5.springboot.models.entitites.Bank;

public interface BankService {

  Bank findBankById(Long id);

  int getTotalOfTransactions(Long bankId);

  Bank save(Bank bank);

  Bank updateTotalOfTransactions(Bank bank);

  Bank updateTotalOfTransactions(Long bankId);
}
