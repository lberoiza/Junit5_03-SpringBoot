package org.lab.junit5.springboot.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lab.junit5.springboot.models.dtos.TransferDetailDTO;
import org.lab.junit5.springboot.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

// WebTestClient es un Wrapper de WebClient que permite realizar pruebas de integración
@Disabled
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerWebTestClientTest {

  private static final String URL_PATH = "http://localhost:8080/api/accounts";

  @Autowired private WebTestClient webTestClient;

  @Autowired private AccountService accountService;

  @Nested
  class TransferTests {
    // Interesante, no es necesario hacer un mock del metodo transferir porque es void y no devuelve
    // nada
    @Test
    void source_account_has_enough_money_then_ok() throws Exception {
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, BigDecimal.ONE);

      String url = URL_PATH + "/transfer";

      webTestClient
          .post()
          .uri(url)
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
          .isEqualTo(LocalDate.now().toString());
    }
  }
}
