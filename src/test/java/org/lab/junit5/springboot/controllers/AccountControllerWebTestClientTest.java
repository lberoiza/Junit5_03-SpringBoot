package org.lab.junit5.springboot.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.*;
import org.lab.junit5.springboot.models.dtos.TransferDetailDTO;
import org.lab.junit5.springboot.models.entitites.Account;
import org.lab.junit5.springboot.testdata.AccountTestDataBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

// WebTestClient es un Wrapper de WebClient que permite realizar pruebas de integración

// @SpringBootTest: Crea un contexto de Spring para realizar pruebas de integración
// webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT: Levanta el servidor en un puerto
// aleatorio
// @Sql: Permite ejecutar scripts SQL antes y después de la clase de prueba
// executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS: Se ejecuta antes de la clase de prueba
// executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS: Se ejecuta después de la clase de prueba

// Al ejecutar los test de integracion en clases anidadas, los cambios se revierten despues de que
// la clase anidada termina, por lo que los cambios, (las transacciones, depositos de dinero) no se
// reflejan en los otras clases lo mismo pasa al agregar o eliminar cuentas.

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(
    scripts = {"/testdata/data-test-cleaner.sql", "/testdata/data-test.sql"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(
    scripts = "/testdata/data-test-cleaner.sql",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class AccountControllerWebTestClientTest {

  private static final String URL_PATH = "/api/accounts";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final BigDecimal TRANSFER_AMOUNT = BigDecimal.TEN;
  private static final BigDecimal START_AMOUNT_ACCOUNT_1 = BigDecimal.valueOf(1000);
  private static final BigDecimal START_AMOUNT_ACCOUNT_2 = BigDecimal.valueOf(2000);

  @Autowired private WebTestClient webTestClient;

  @Nested
  @Order(1)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class TransferTests {

    @Test
    @Order(1)
    void source_account_has_enough_money_then_ok_expectBody() throws Exception {
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, TRANSFER_AMOUNT);
      Map<String, Object> expectedResponse = createResponseMap(transferDetailDTO);

      WebTestClient.BodyContentSpec bodyContentSpec =
          webTestClient
              .post()
              .uri(URL_PATH + "/transfer")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(transferDetailDTO)
              .exchange() // Realiza la llamada y lo que venga despues será la respuesta
              .expectStatus()
              .isOk()
              .expectHeader()
              .contentType(MediaType.APPLICATION_JSON)
              .expectBody();

      // 1.- Usando JsonPath para validar la respuesta
      assertWithJsonPath(bodyContentSpec, transferDetailDTO);

      // 2.- Probando el Json completo de la respuesta
      // a.- Usando un Map y convirtiendolo a JSON
      bodyContentSpec.json(objectMapper.writeValueAsString(expectedResponse));
      // b.- Usando un String con el JSON
      bodyContentSpec.json(createResponseAsJsonString(transferDetailDTO));

      // 3.- probando el consumeWith
      assertConsumeWith(bodyContentSpec);
    }

    private void assertWithJsonPath(
        WebTestClient.BodyContentSpec bodyContentSpec, TransferDetailDTO transferDetailDTO) {
      bodyContentSpec
          .jsonPath("$.message")
          .isNotEmpty()
          .jsonPath("$.message")
          .value(is("Transfer successful"))
          .jsonPath("$.data.sourceAccountId")
          .isEqualTo(transferDetailDTO.sourceAccountId())
          .jsonPath("$.data.targetAccountId")
          .value(
              value -> assertThat(value).isEqualTo(transferDetailDTO.targetAccountId().intValue()))
          .jsonPath("$.date")
          .isEqualTo(LocalDate.now().toString());
    }

    private Map<String, Object> createResponseMap(TransferDetailDTO transferDetailDTO) {
      Map<String, Object> response = new HashMap<>();
      response.put("message", "Transfer successful");
      response.put("status", "ok");
      response.put("date", LocalDate.now().toString());
      response.put("data", transferDetailDTO);
      return response;
    }

    private String createResponseAsJsonString(TransferDetailDTO transferDetailDTO) {
      String jsonResponse =
          """
            {
              "message": "Transfer successful",
              "status": "ok",
              "date": "%s",
              "data": {
                "sourceAccountId": %d,
                "targetAccountId": %d,
                "bankId": %d,
                "amount": %s
              }
            }
            """;

      return jsonResponse.formatted(
          LocalDate.now().toString(),
          transferDetailDTO.sourceAccountId(),
          transferDetailDTO.targetAccountId(),
          transferDetailDTO.bankId(),
          transferDetailDTO.amount());
    }

    private void assertConsumeWith(WebTestClient.BodyContentSpec bodyContentSpec) {
      bodyContentSpec.consumeWith(
          response -> {
            try {
              JsonNode jsonNode = objectMapper.readTree(response.getResponseBody());
              assertThat(jsonNode.path("message").asText()).isEqualTo("Transfer successful");
              assertThat(jsonNode.path("date").asText()).isEqualTo(LocalDate.now().toString());
              assertThat(jsonNode.path("data").path("amount").asText())
                  .isEqualTo(TRANSFER_AMOUNT.toString());
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }

  @Nested
  @Order(2)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class DetailTests {

    @Test
    @Order(1)
    void get_account_by_account_number_account_1_then_ok() throws JsonProcessingException {
      Account expectedAccount =
          new Account()
              .setId(1L)
              .setAccountNumber("123456")
              .setBalance(START_AMOUNT_ACCOUNT_1)
              .setOwner("Juan Perez");

      webTestClient
          .get()
          .uri(URL_PATH + "/123456")
          .exchange()
          .expectStatus()
          .isOk()
          .expectHeader()
          .contentType(MediaType.APPLICATION_JSON)
          .expectBody()
          .jsonPath("$.id")
          .isEqualTo(expectedAccount.getId())
          .jsonPath("$.accountNumber")
          .isEqualTo(expectedAccount.getAccountNumber())
          .jsonPath("$.balance")
          .isEqualTo(expectedAccount.getBalance().intValue())
          .jsonPath("$.owner")
          .isEqualTo(expectedAccount.getOwner())
          .json(objectMapper.writeValueAsString(expectedAccount));
    }

    @Test
    @Order(2)
    void get_account_by_account_number_account_2_then_ok() {
      Account expectedAccount =
          new Account()
              .setId(2L)
              .setAccountNumber("654321")
              .setBalance(START_AMOUNT_ACCOUNT_2)
              .setOwner("Maria Lopez");

      webTestClient
          .get()
          .uri(URL_PATH + "/" + expectedAccount.getAccountNumber())
          .exchange()
          .expectStatus()
          .isOk()
          .expectHeader()
          .contentType(MediaType.APPLICATION_JSON)

          // Expected Body con Tipo de dato
          .expectBody(Account.class)
          .consumeWith(
              response -> {
                Account account = response.getResponseBody();
                assertThat(account).isNotNull();
                assertThat(account.getId()).isEqualTo(expectedAccount.getId());
                assertThat(account.getAccountNumber())
                    .isEqualTo(expectedAccount.getAccountNumber());
                assertThat(account.getBalance().setScale(2, RoundingMode.HALF_UP))
                    .isEqualTo(expectedAccount.getBalance().setScale(2, RoundingMode.HALF_UP));
                assertThat(account.getOwner()).isEqualTo(expectedAccount.getOwner());
              });
    }
  }

  @Nested
  @Order(3)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class List {

    @Test
    @Order(1)
    void get_all_accounts_then_ok_json_path() {
      WebTestClient.ResponseSpec responseSpec =
          webTestClient
              .get()
              .uri(URL_PATH)
              .exchange()
              .expectStatus()
              .isOk()
              .expectHeader()
              .contentType(MediaType.APPLICATION_JSON);

      // 1.- Usando JsonPath
      assertWithJsonPath(responseSpec.expectBody());
    }

    private void assertWithJsonPath(WebTestClient.BodyContentSpec bodyContentSpec) {
      bodyContentSpec
          .jsonPath("$.[0].id")
          .isEqualTo(1)
          .jsonPath("$.[0].accountNumber")
          .isEqualTo(123456)
          .jsonPath("$.[0].owner")
          .isEqualTo("Juan Perez")
          .jsonPath("$.[0].balance")
          .isEqualTo(START_AMOUNT_ACCOUNT_1.intValue())
          .jsonPath("$.[1].id")
          .isEqualTo(2)
          .jsonPath("$.[1].accountNumber")
          .isEqualTo(654321)
          .jsonPath("$.[1].owner")
          .isEqualTo("Maria Lopez")
          .jsonPath("$.[1].balance")
          .isEqualTo(START_AMOUNT_ACCOUNT_2.intValue());
    }

    @Test
    @Order(2)
    void get_all_accounts_then_ok_consume_with() {
      WebTestClient.ResponseSpec responseSpec =
          webTestClient
              .get()
              .uri(URL_PATH)
              .exchange()
              .expectStatus()
              .isOk()
              .expectHeader()
              .contentType(MediaType.APPLICATION_JSON);

      // 2.- Usando consumeWith
      assertConsumeWith(responseSpec.expectBodyList(Account.class));
    }

    private void assertConsumeWith(WebTestClient.ListBodySpec<Account> accountListBodySpec) {
      accountListBodySpec
          .hasSize(2)
          .value(hasSize(2)) // matcher
          .consumeWith(
              response -> {
                Account account1 = Objects.requireNonNull(response.getResponseBody()).getFirst();
                assertThat(account1).isNotNull();
                assertThat(account1.getId()).isEqualTo(1);
                assertThat(account1.getAccountNumber()).isEqualTo("123456");
                assertThat(account1.getOwner()).isEqualTo("Juan Perez");
                assertThat(account1.getBalance())
                    .isEqualTo(START_AMOUNT_ACCOUNT_1.setScale(2, RoundingMode.HALF_UP));

                Account account2 = response.getResponseBody().get(1);
                assertThat(account2).isNotNull();
                assertThat(account2.getId()).isEqualTo(2);
                assertThat(account2.getAccountNumber()).isEqualTo("654321");
                assertThat(account2.getOwner()).isEqualTo("Maria Lopez");
                assertThat(account2.getBalance())
                    .isEqualTo(START_AMOUNT_ACCOUNT_2.setScale(2, RoundingMode.HALF_UP));

                assertThat(response.getResponseBody())
                    .extracting(Account::getAccountNumber)
                    .containsExactlyInAnyOrder("123456", "654321");
              });
    }
  }

  @Nested
  @Order(4)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class Save {

    private final Account newAccount = AccountTestDataBuilder.random().withId(null).build();

    @Test
    @Order(1)
    void then_ok_json_path() {
      WebTestClient.ResponseSpec responseSpec =
          webTestClient
              .post()
              .uri(URL_PATH + "/create")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(newAccount)
              .exchange()
              .expectStatus()
              .isCreated()
              .expectHeader()
              .contentType(MediaType.APPLICATION_JSON);

      // 1.- Usando JsonPath
      assertWithJsonPath(responseSpec.expectBody());
    }

    private void assertWithJsonPath(WebTestClient.BodyContentSpec bodyContentSpec) {
      bodyContentSpec
          .jsonPath("$.id")
          .isEqualTo(3L)
          .jsonPath("$.accountNumber")
          .isEqualTo(newAccount.getAccountNumber())
          .jsonPath("$.owner")
          .isEqualTo(newAccount.getOwner())
          .jsonPath("$.balance")
          .isEqualTo(newAccount.getBalance().intValue());
    }

    @Test
    @Order(2)
    void then_ok_consume_with() {
      WebTestClient.ResponseSpec responseSpec =
          webTestClient
              .post()
              .uri(URL_PATH + "/create")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(newAccount)
              .exchange()
              .expectStatus()
              .isCreated()
              .expectHeader()
              .contentType(MediaType.APPLICATION_JSON);

      // 2.- Usando consumeWith
      assertConsumeWith(responseSpec.expectBody(Account.class));
    }

    private void assertConsumeWith(WebTestClient.BodySpec<Account, ?> bodySpec) {
      bodySpec.consumeWith(
          response -> {
            Account account1 = response.getResponseBody();
            assertThat(account1).isNotNull();
            assertThat(account1.getId()).isEqualTo(4L);
            assertThat(account1.getAccountNumber()).isEqualTo(newAccount.getAccountNumber());
            assertThat(account1.getOwner()).isEqualTo(newAccount.getOwner());
            assertThat(account1.getBalance().setScale(2, RoundingMode.HALF_UP))
                .isEqualTo(newAccount.getBalance().setScale(2, RoundingMode.HALF_UP));
          });
    }
  }

  @Nested
  @Order(5)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class Delete {

    @Test
    @Order(1)
    void get_all_accounts_then_ok_json_path() {
      assertCountAccounts(2);

      webTestClient
          .delete()
          .uri(URL_PATH + "/1")
          .exchange()
          .expectStatus()
          .isNoContent()
          .expectBody()
          .isEmpty();

      assertCountAccounts(1);

      // como tenemos implementado el manejo de excepciones en el controlador, no se lanza una
      // excepción 500
      // se lanza una excepción 404
      webTestClient.get().uri(URL_PATH + "/1").exchange().expectStatus().isNotFound();

      // si no tenemos implementado el manejo de excepciones en el controlador, se lanza una
      // excepción 500
      // webTestClient.get().uri(URL_PATH + "/1").exchange().expectStatus().is5xxServerError();

    }

    private void assertCountAccounts(int expectedCount) {
      webTestClient
          .get()
          .uri(URL_PATH)
          .exchange()
          .expectHeader()
          .contentType(MediaType.APPLICATION_JSON)
          .expectStatus()
          .isOk()
          .expectBodyList(Account.class)
          .hasSize(expectedCount);
    }
  }
}
