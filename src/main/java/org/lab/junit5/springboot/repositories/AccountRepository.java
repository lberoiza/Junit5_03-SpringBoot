package org.lab.junit5.springboot.repositories;

import org.lab.junit5.springboot.models.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {}
