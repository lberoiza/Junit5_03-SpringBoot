package org.lab.junit5.springboot.models;

import lombok.Data;
import lombok.experimental.Accessors;
import org.lab.junit5.springboot.exceptions.InsufficientMoneyException;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class Account {
  private Long id;
  private String accountNumber;
  private String owner;
  private BigDecimal balance;

  public void deposit(BigDecimal amount) {
    balance = balance.add(amount);
  }

  public void withdraw(BigDecimal amount) {
    BigDecimal newBalance = balance.subtract(amount);
    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new InsufficientMoneyException(this, amount);
    }
    balance = balance.subtract(amount);
  }
}