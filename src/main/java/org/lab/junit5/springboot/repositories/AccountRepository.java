package org.lab.junit5.springboot.repositories;

import org.lab.junit5.springboot.models.Account;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository {
  List<Account> findAll();

  Optional<Account> findById(Long id);

  Account save(Account account);
}
