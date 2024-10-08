package org.lab.junit5.springboot.exceptions;

import java.math.BigDecimal;
import org.lab.junit5.springboot.models.Account;

public class InsufficientMoneyException extends AccountException{

  private static final String message = "Insufficient money in account nr: %s. Current balance: %.2f, requested amount: %.2f";

  public InsufficientMoneyException(Account account, BigDecimal amount) {
    super(createErrorMessage(account, amount));
  }

  private static String createErrorMessage(Account account, BigDecimal amount) {
    return message.formatted(account.getAccountNumber(), account.getBalance(), amount);
  }

}
