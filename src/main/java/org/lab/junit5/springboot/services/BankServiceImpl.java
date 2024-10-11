package org.lab.junit5.springboot.services;

import lombok.AllArgsConstructor;
import org.lab.junit5.springboot.exceptions.BankNotFoundByIdException;
import org.lab.junit5.springboot.models.entitites.Bank;
import org.lab.junit5.springboot.repositories.BankRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BankServiceImpl implements BankService {

  private final BankRepository bankRepository;

  public Bank findBankById(Long bankId) {
    return bankRepository.findById(bankId).orElseThrow(() -> new BankNotFoundByIdException(bankId));
  }

  @Override
  public Bank save(Bank bank) {
    return bankRepository.save(bank);
  }

  @Override
  public Bank updateTotalOfTransactions(Long bankId) {
    Bank bank = findBankById(bankId);
    return updateTotalOfTransactions(bank);
  }

  @Override
  public Bank updateTotalOfTransactions(Bank bank) {
    bank.setTotalOfTransactions(bank.getTotalOfTransactions() + 1);
    return save(bank);
  }

  @Override
  public int getTotalOfTransactions(Long bankId) {
    return findBankById(bankId).getTotalOfTransactions();
  }
}
