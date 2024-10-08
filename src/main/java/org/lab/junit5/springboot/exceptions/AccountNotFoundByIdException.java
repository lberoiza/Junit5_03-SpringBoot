package org.lab.junit5.springboot.exceptions;


public class AccountNotFoundByIdException extends AccountException{

  private static final String message = "Account with id '%s' not found.";

  public AccountNotFoundByIdException(Long accountId) {
    super(message.formatted(accountId));
  }

}
