package org.lab.junit5.springboot.repositories;

import org.lab.junit5.springboot.models.entitites.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByAccountNumber(String accountNumber);
}
