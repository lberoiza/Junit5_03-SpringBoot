package org.lab.junit5.springboot.exceptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BankException extends RuntimeException {

  public BankException(String message) {
    super(message);
    log.error(message);
  }

}
