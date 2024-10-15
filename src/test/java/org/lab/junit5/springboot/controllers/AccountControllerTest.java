package org.lab.junit5.springboot.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lab.junit5.springboot.exceptions.AccountInsufficientMoneyException;
import org.lab.junit5.springboot.exceptions.AccountNotFoundByNumberException;
import org.lab.junit5.springboot.exceptions.BankNotFoundByIdException;
import org.lab.junit5.springboot.models.dtos.TransferDetailDTO;
import org.lab.junit5.springboot.models.entitites.Account;
import org.lab.junit5.springboot.services.AccountService;
import org.lab.junit5.springboot.testdata.AccountTestDataBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

  private static final String CONTROLLER_PATH = "/api/accounts";

  @Autowired private MockMvc mockMvc;

  @MockBean private AccountService accountService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Nested
  class GetAccountByAccountNumberTests {
    @Test
    void should_find_account_then_ok_and_account_details() throws Exception {
      Account testAccount = AccountTestDataBuilder.random().build();
      doReturn(testAccount)
          .when(accountService)
          .findAccountByAccountNumber(testAccount.getAccountNumber());

      String url = CONTROLLER_PATH + "/" + testAccount.getAccountNumber();
      mockMvc
          .perform(get(url))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(testAccount.getId()))
          .andExpect(jsonPath("$.accountNumber").value(testAccount.getAccountNumber()))
          .andExpect(jsonPath("$.owner").value(testAccount.getOwner()))
          .andExpect(jsonPath("$.balance").value(testAccount.getBalance()));

      verify(accountService, times(1)).findAccountByAccountNumber(testAccount.getAccountNumber());
    }

    @Test
    void should_not_find_account_then_exception() throws Exception {
      String testAccountNumber = "123";
      doThrow(new AccountNotFoundByNumberException(testAccountNumber))
          .when(accountService)
          .findAccountByAccountNumber(testAccountNumber);

      String url = CONTROLLER_PATH + "/" + testAccountNumber;
      mockMvc.perform(get(url)).andExpect(status().isNotFound());

      verify(accountService, times(1)).findAccountByAccountNumber(testAccountNumber);
    }
  }

  @Nested
  class TransferTests {
    // Interesante, no es necesario hacer un mock del metodo transferir porque es void y no devuelve
    // nada
    @Test
    void source_account_has_enough_money_then_ok() throws Exception {
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, BigDecimal.ONE);

      String url = CONTROLLER_PATH + "/transfer";

      mockMvc
          .perform(
              post(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(transferDetailDTO)))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.message").value("Transfer successful"))
          .andExpect(jsonPath("$.status").value("ok"))
          .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
          .andExpect(jsonPath("$.data.sourceAccountId").value(transferDetailDTO.sourceAccountId()))
          .andExpect(jsonPath("$.data.targetAccountId").value(transferDetailDTO.targetAccountId()));
    }

    @Test
    void source_account_has_enough_money_then_ok_check_whole_json() throws Exception {
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, BigDecimal.ONE);

      Map<String, Object> response = new HashMap<>();
      response.put("message", "Transfer successful");
      response.put("status", "ok");
      response.put("date", LocalDate.now().toString());
      response.put("data", transferDetailDTO);

      String url = CONTROLLER_PATH + "/transfer";

      mockMvc
          .perform(
              post(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(transferDetailDTO)))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    @Test
    void source_account_has_not_enough_money_then_InsufficientMoneyException() throws Exception {
      Account sourceAccount = AccountTestDataBuilder.random().build();
      TransferDetailDTO transferDetailDTO =
          new TransferDetailDTO(
              sourceAccount.getId(), 2L, 1L, sourceAccount.getBalance().multiply(BigDecimal.TEN));

      doThrow(new AccountInsufficientMoneyException(sourceAccount, transferDetailDTO.amount()))
          .when(accountService)
          .transfer(
              transferDetailDTO.sourceAccountId(),
              transferDetailDTO.targetAccountId(),
              transferDetailDTO.amount(),
              transferDetailDTO.bankId());

      String url = CONTROLLER_PATH + "/transfer";

      mockMvc
          .perform(
              post(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(transferDetailDTO)))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("error"))
          .andExpect(
              jsonPath("$.message")
                  .value(
                      "Insufficient money in account nr: %s. Current balance: %.2f, requested amount: %.2f"
                          .formatted(
                              sourceAccount.getAccountNumber(),
                              sourceAccount.getBalance(),
                              transferDetailDTO.amount())));
    }

    @Test
    void TransferDetailDTO_incomplete_then_Exception() throws Exception {
      String url = CONTROLLER_PATH + "/transfer";

      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, null);

      mockMvc
          .perform(
              post(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(transferDetailDTO)))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("error"))
          .andExpect(jsonPath("$.message").value("Transfer details are required"));
    }

    @Test
    void bank_not_found_then_Exception() throws Exception {
      String url = CONTROLLER_PATH + "/transfer";
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, BigDecimal.ONE);

      doThrow(new BankNotFoundByIdException(transferDetailDTO.bankId()))
          .when(accountService)
          .transfer(
              transferDetailDTO.sourceAccountId(),
              transferDetailDTO.targetAccountId(),
              transferDetailDTO.amount(),
              transferDetailDTO.bankId());

      mockMvc
          .perform(
              post(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(transferDetailDTO)))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("error"))
          .andExpect(
              jsonPath("$.message")
                  .value("Bank with id '%s' not found.".formatted(transferDetailDTO.bankId())));
    }

    @Test
    void then_Exception() throws Exception {
      String url = CONTROLLER_PATH + "/transfer";
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, BigDecimal.ONE);

      doThrow(new IllegalArgumentException("Unknown exception"))
          .when(accountService)
          .transfer(
              transferDetailDTO.sourceAccountId(),
              transferDetailDTO.targetAccountId(),
              transferDetailDTO.amount(),
              transferDetailDTO.bankId());

      mockMvc
          .perform(
              post(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(transferDetailDTO)))
          .andExpect(status().isInternalServerError());
    }
  }

  @Nested
  class CreateAccountTest {
    @Test
    void return_account_with_id() throws Exception {
      Long generatedId = 5L;
      Account requestAccount = AccountTestDataBuilder.random().withId(null).build();

      // Se devuelve un clon del argumento
      // para que de esta forma al usar verify se puede usar el argumento original
      // si se modifica, mockito piensa que el argumento inicial es el modificado dentro
      // del lamnda
      doAnswer(invocation -> cloneAccount(invocation.getArgument(0)).setId(generatedId))
          .when(accountService)
          .save(any(Account.class));

      String url = CONTROLLER_PATH + "/create";
      mockMvc
          .perform(
              post(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(requestAccount)))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(generatedId))
          .andExpect(jsonPath("$.accountNumber").value(requestAccount.getAccountNumber()))
          .andExpect(jsonPath("$.owner").value(requestAccount.getOwner()))
          .andExpect(jsonPath("$.balance").value(requestAccount.getBalance()));

      verify(accountService, times(1)).save(requestAccount);
    }
  }

  @Nested
  class updateAccountTest {
    @Test
    void return_account() throws Exception {
      Account requestAccount = AccountTestDataBuilder.random().build();

      when(accountService.save(requestAccount)).thenReturn(requestAccount);

      String url = CONTROLLER_PATH + "/update";
      mockMvc
          .perform(
              put(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(requestAccount)))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(requestAccount.getId()))
          .andExpect(jsonPath("$.accountNumber").value(requestAccount.getAccountNumber()))
          .andExpect(jsonPath("$.owner").value(requestAccount.getOwner()))
          .andExpect(jsonPath("$.balance").value(requestAccount.getBalance()));

      verify(accountService, times(1)).save(requestAccount);
    }
  }

  private Account cloneAccount(Account account) {
    return new Account()
        .setId(account.getId())
        .setAccountNumber(account.getAccountNumber())
        .setOwner(account.getOwner())
        .setBalance(account.getBalance());
  }
}
