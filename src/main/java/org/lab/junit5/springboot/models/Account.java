package org.lab.junit5.springboot.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;
import org.lab.junit5.springboot.exceptions.InsufficientMoneyException;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "accounts")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String accountNumber;

  @Column(nullable = false)
  private String owner;

  @Column(
      nullable = false,
      precision = 10,
      scale = 2,
      columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
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
