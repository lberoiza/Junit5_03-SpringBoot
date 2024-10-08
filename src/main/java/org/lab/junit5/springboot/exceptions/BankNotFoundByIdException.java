package org.lab.junit5.springboot.exceptions;


public class BankNotFoundByIdException extends AccountException{

  private static final String message = "Bank with id '%s' not found.";

  public BankNotFoundByIdException(Long bankId) {
    super(message.formatted(bankId));
  }

}
