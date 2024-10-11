package org.lab.junit5.springboot.models.dtos;

import java.math.BigDecimal;

public record TransferDetailDTO(
    Long sourceAccountId, Long targetAccountId, Long bankId, BigDecimal amount) {}
