package org.lab.junit5.springboot.repositories;

import java.util.List;
import java.util.Optional;
import org.lab.junit5.springboot.models.Bank;

public interface BankRepository {
  List<Bank> findAll();

  Optional<Bank> findById(Long id);

  Bank save(Bank bank);
}
