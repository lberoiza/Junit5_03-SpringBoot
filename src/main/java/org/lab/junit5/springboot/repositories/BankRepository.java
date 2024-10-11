package org.lab.junit5.springboot.repositories;

import org.lab.junit5.springboot.models.entitites.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepository extends JpaRepository<Bank, Long> {}
