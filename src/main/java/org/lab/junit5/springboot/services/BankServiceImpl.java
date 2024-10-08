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
}
