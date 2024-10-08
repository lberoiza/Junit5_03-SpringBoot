package org.lab.junit5.springboot.services;

import org.lab.junit5.springboot.models.Bank;

public interface BankService {

  Bank findBankById(Long id);
  Bank save(Bank bank);
  Bank updateTotalOfTransfers(Bank bank);
  Bank updateTotalOfTransfers(Long bankId);

}
