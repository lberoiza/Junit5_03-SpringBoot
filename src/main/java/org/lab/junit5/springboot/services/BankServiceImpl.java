package org.lab.junit5.springboot.services;

import lombok.AllArgsConstructor;
import org.lab.junit5.springboot.exceptions.BankNotFoundByIdException;
import org.lab.junit5.springboot.models.Bank;
import org.lab.junit5.springboot.repositories.BankRepository;

@AllArgsConstructor
public class BankServiceImpl implements BankService {

  private final BankRepository bankRepository;

  public Bank findBankById(Long bankId) {
    return bankRepository.findById(bankId)
        .orElseThrow(() -> new BankNotFoundByIdException(bankId));
  }

  @Override
  public Bank save(Bank bank) {
    return bankRepository.save(bank);
  }

  @Override
  public Bank updateTotalOfTransfers(Long bankId) {
    Bank bank = findBankById(bankId);
    return updateTotalOfTransfers(bank);
  }

  @Override
  public Bank updateTotalOfTransfers(Bank bank) {
    bank.setTotalOfTransfers(bank.getTotalOfTransfers() + 1);
    return save(bank);
  }
}
