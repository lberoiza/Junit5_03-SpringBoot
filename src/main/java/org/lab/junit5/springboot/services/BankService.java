package org.lab.junit5.springboot.services;

import org.lab.junit5.springboot.models.Bank;

public interface BankService {

  Bank findBankById(Long id);

}
