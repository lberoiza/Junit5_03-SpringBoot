package org.lab.junit5.springboot.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.lab.junit5.springboot.exceptions.AccountNotFoundByNumberException;
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

  @Test
  void getAccountByAccountNumber_should_find_account_then_ok_and_account_details()
      throws Exception {
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
  void getAccountByAccountNumber_should_not_find_account_then_exception() throws Exception {
    String testAccountNumber = "123";
    doThrow(new AccountNotFoundByNumberException(testAccountNumber))
        .when(accountService)
        .findAccountByAccountNumber(testAccountNumber);

    String url = CONTROLLER_PATH + "/" + testAccountNumber;
    mockMvc.perform(get(url)).andExpect(status().isNotFound());

    verify(accountService, times(1)).findAccountByAccountNumber(testAccountNumber);
  }

  @Test
  void transfer() {}

  @Test
  void createAccount() {}

  @Test
  void updateAccount() {}
}
