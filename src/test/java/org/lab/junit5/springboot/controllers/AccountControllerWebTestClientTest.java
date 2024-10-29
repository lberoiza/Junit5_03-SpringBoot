package org.lab.junit5.springboot.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lab.junit5.springboot.models.dtos.TransferDetailDTO;
import org.lab.junit5.springboot.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

// WebTestClient es un Wrapper de WebClient que permite realizar pruebas de integración
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/testdata/data-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(
    scripts = "/testdata/data-test-cleaner.sql",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AccountControllerWebTestClientTest {

  private static final String URL_PATH = "/api/accounts";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private WebTestClient webTestClient;

  @Autowired private AccountService accountService;

  @Nested
  class TransferTests {

    @Test
    void source_account_has_enough_money_then_ok() throws Exception {
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, BigDecimal.ONE);
      Map<String, Object> expectedResponse = createResponseMap(transferDetailDTO);

      webTestClient
          .post()
          .uri(URL_PATH + "/transfer")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(transferDetailDTO)
          .exchange() // Realiza la llamada y lo que venga despues será la respuesta
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.message")
          .isNotEmpty()
          .jsonPath("$.message")
          .value(is("Transfer successful"))
          // otra forma isequalTo
          .jsonPath("$.data.sourceAccountId")
          .isEqualTo(transferDetailDTO.sourceAccountId())
          // otra forma lambda
          .jsonPath("$.data.targetAccountId")
          .value(
              value -> assertThat(value).isEqualTo(transferDetailDTO.targetAccountId().intValue()))
          // usando isEqualTo
          .jsonPath("$.date")
          .isEqualTo(LocalDate.now().toString())
          .json(objectMapper.writeValueAsString(expectedResponse));
    }
  }

  private Map<String, Object> createResponseMap(TransferDetailDTO transferDetailDTO) {
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Transfer successful");
    response.put("status", "ok");
    response.put("date", LocalDate.now().toString());
    response.put("data", transferDetailDTO);
    return response;
  }
}