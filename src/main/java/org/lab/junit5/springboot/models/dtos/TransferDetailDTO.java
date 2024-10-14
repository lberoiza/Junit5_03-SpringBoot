package org.lab.junit5.springboot.models.dtos;

import java.math.BigDecimal;

public record TransferDetailDTO(
    Long sourceAccountId, Long targetAccountId, Long bankId, BigDecimal amount) {

  public boolean isNotValid() {
    return sourceAccountId == null || targetAccountId == null || bankId == null || amount == null;
  }
}
