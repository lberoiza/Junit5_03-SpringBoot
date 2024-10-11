package org.lab.junit5.springboot.exceptions;

public class AccountNotFoundByNumberException extends AccountException {

  private static final String message = "Account with number '%s' not found.";

  public AccountNotFoundByNumberException(String accountNumber) {
    super(message.formatted(accountNumber));
  }
}
