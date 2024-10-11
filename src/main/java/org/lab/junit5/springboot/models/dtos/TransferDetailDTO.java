package org.lab.junit5.springboot.models.dtos;

import org.lab.junit5.springboot.models.entitites.Account;

public record TransferDetailDTO(
    Account sourceAccount, Account targetAccount, Long bankId, String amount) {}
