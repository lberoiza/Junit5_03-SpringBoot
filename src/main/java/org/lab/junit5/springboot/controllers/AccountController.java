package org.lab.junit5.springboot.controllers;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.lab.junit5.springboot.exceptions.AccountException;
import org.lab.junit5.springboot.exceptions.AccountNotFoundByNumberException;
import org.lab.junit5.springboot.exceptions.BankException;
import org.lab.junit5.springboot.models.dtos.TransferDetailDTO;
import org.lab.junit5.springboot.models.entitites.Account;
import org.lab.junit5.springboot.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;

  @Autowired
  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public ResponseEntity<List<Account>> getAllAccounts() {
    return ResponseEntity.ok(accountService.findAllAccounts());
  }

  @GetMapping("/{accountNumber}")
  public ResponseEntity<Account> getAccountByAccountNumber(@PathVariable String accountNumber) {
    try {
      return ResponseEntity.ok(accountService.findAccountByAccountNumber(accountNumber));
    } catch (AccountNotFoundByNumberException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping("/transfer")
  public ResponseEntity<Map<String, Object>> transfer(
      @RequestBody TransferDetailDTO transferDetailDTO) {
    Map<String, Object> response = new HashMap<>();

    if (transferDetailDTO.isNotValid()) {
      response.put("status", "error");
      response.put("message", "Transfer details are required");
      return ResponseEntity.badRequest().body(response);
    }

    try {
      accountService.transfer(
          transferDetailDTO.sourceAccountId(),
          transferDetailDTO.targetAccountId(),
          transferDetailDTO.amount(),
          transferDetailDTO.bankId());
      response.put("message", "Transfer successful");
      response.put("status", "ok");
      response.put("date", LocalDate.now());
      response.put("data", transferDetailDTO);
      return ResponseEntity.ok(response);
    } catch (AccountException | BankException e) {
      response.put("status", "error");
      response.put("message", e.getMessage());
      return ResponseEntity.badRequest().body(response);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping("/create")
  public ResponseEntity<Account> createAccount(@RequestBody Account account) {
    return Optional.of(account)
        .map(accountService::save)
        .map(
            savedAccount -> {
              try {
                URI location = new URI("/accounts/" + savedAccount.getId());
                return ResponseEntity.created(location).body(savedAccount);
              } catch (URISyntaxException e) {
                return null;
              }
            })
        .orElse(ResponseEntity.badRequest().build());
  }

  @PutMapping("/update")
  public ResponseEntity<Account> updateAccount(@RequestBody Account account) {
    return Optional.of(account)
        .map(accountService::save)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.badRequest().build());
  }

  @DeleteMapping("/{accountId}")
  public ResponseEntity<Account> deleteAccount(@PathVariable Long accountId) {
    accountService.delete(accountId);
    return ResponseEntity.noContent().build();
  }
}
