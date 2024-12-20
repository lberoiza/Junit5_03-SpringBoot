package org.lab.junit5.springboot.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.lab.junit5.springboot.models.dtos.TransferDetailDTO;
import org.lab.junit5.springboot.models.entitites.Account;
import org.lab.junit5.springboot.testdata.AccountTestDataBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

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
class AccountControllerTestRestTemplateTest {

  private static final String URL_PATH = "/api/accounts";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final BigDecimal TRANSFER_AMOUNT = BigDecimal.TEN;
  private static final BigDecimal START_AMOUNT_ACCOUNT_1 = BigDecimal.valueOf(1000);
  private static final BigDecimal START_AMOUNT_ACCOUNT_2 = BigDecimal.valueOf(2000);

  @Autowired private TestRestTemplate restTemplateClient;

  @LocalServerPort private int applicationPort;

  @BeforeEach
  void setUp() {
    System.out.println("### Starting test on port: " + applicationPort);
  }

  // Se puede usar la funcion createUri para crear la URL de la petición
  // usando el puerto random generado por SpringBootTest
  private String createUri(String endpoint) {
    return "http://localhost:" + applicationPort + URL_PATH + endpoint;
  }

  @Nested
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  @Order(1)
  class TransferTest {

    @Test
    @Order(1)
    void test_as_map() {
      // Given
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, TRANSFER_AMOUNT);

      // When
      ResponseEntity<Map> response =
          restTemplateClient.postForEntity(createUri("/transfer"), transferDetailDTO, Map.class);

      // usa pruebas suaves para verificar todas las condiciones
      // y no detenerse en la primera falla
      SoftAssertions softly = new SoftAssertions();

      // Then
      softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      softly
          .assertThat(response.getHeaders().getContentType())
          .isEqualTo(MediaType.APPLICATION_JSON);
      softly.assertThat(response.getBody().get("status")).isEqualTo("ok");
      softly.assertThat(response.getBody().get("message")).isEqualTo("Transfer successful");
      softly
          .assertThat(response.getBody().get("date"))
          .isNotNull()
          .isEqualTo(LocalDate.now().toString());
      softly.assertThat(((Map) response.getBody().get("data")).get("sourceAccountId")).isEqualTo(1);
      softly.assertThat(((Map) response.getBody().get("data")).get("targetAccountId")).isEqualTo(2);
      softly
          .assertThat(((Map) response.getBody().get("data")).get("amount"))
          .isEqualTo(TRANSFER_AMOUNT.intValue());

      softly.assertAll();
    }

    @Test
    @Order(2)
    void test_as_json() throws JsonProcessingException {
      // Given
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, TRANSFER_AMOUNT);

      // When
      ResponseEntity<String> response =
          restTemplateClient.postForEntity(URL_PATH + "/transfer", transferDetailDTO, String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

      JsonNode jsonNode = objectMapper.readTree(response.getBody());
      assertThat(jsonNode.path("status").asText()).isEqualTo("ok");
      assertThat(jsonNode.path("message").asText()).isEqualTo("Transfer successful");
      assertThat(jsonNode.path("date").asText()).isNotNull().isEqualTo(LocalDate.now().toString());
      assertThat(jsonNode.path("data").path("sourceAccountId").asLong()).isEqualTo(1);
      assertThat(jsonNode.path("data").path("targetAccountId").asLong()).isEqualTo(2);
      assertThat(jsonNode.path("data").path("amount").asDouble())
          .isEqualTo(TRANSFER_AMOUNT.doubleValue());
    }

    @Test
    @Order(3)
    void expected_error_Insufficient_money() {
      BigDecimal expenseAmount = BigDecimal.valueOf(9999.00);

      // Given
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, expenseAmount);

      // When
      var response =
          restTemplateClient.postForEntity(URL_PATH + "/transfer", transferDetailDTO, Map.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().get("status")).isEqualTo("error");
      assertThat(response.getBody().get("message"))
          .isEqualTo(
              "Insufficient money in account nr: %s. Current balance: %.2f, requested amount: %.2f"
                  .formatted(
                      "123456",
                      START_AMOUNT_ACCOUNT_1.subtract(TRANSFER_AMOUNT.multiply(BigDecimal.TWO)),
                      expenseAmount));
    }
  }

  @Nested
  @Order(2)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class DetailTest {

    @Test
    @Order(1)
    void test_find_by_id() {
      // Given
      Account expectedAccount =
          new Account()
              .setId(1L)
              .setAccountNumber("123456")
              .setOwner("Juan Perez")
              .setBalance(BigDecimal.valueOf(1000.00).setScale(2, RoundingMode.HALF_UP));

      // When
      ResponseEntity<Account> response =
          restTemplateClient.getForEntity(
              createUri("/" + expectedAccount.getAccountNumber()), Account.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
      Account account = response.getBody();

      // Prueba campo por campo
      assertThat(account).isNotNull();
      assertThat(account.getId()).isEqualTo(expectedAccount.getId());
      assertThat(account.getAccountNumber()).isEqualTo(expectedAccount.getAccountNumber());
      assertThat(account.getOwner()).isEqualTo(expectedAccount.getOwner());
      assertThat(account.getBalance().setScale(2, RoundingMode.HALF_UP))
          .isEqualTo(expectedAccount.getBalance().setScale(2, RoundingMode.HALF_UP));

      // Prueba completa de igualdad de objetos
      assertThat(account).isEqualTo(expectedAccount);
    }

    @Test
    @Order(2)
    void test_find_all() throws JsonProcessingException {
      // When
      ResponseEntity<Account[]> response =
          restTemplateClient.getForEntity(URL_PATH, Account[].class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

      assertThat(response.getBody()).isNotNull();

      // Prueba de lista de cuentas
      List<Account> accounts = Arrays.asList(response.getBody());
      assertAccountList(accounts);

      // prueba la lista usando JsonNode
      JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(accounts));
      assertAccountJsonNode(jsonNode);
    }

    private void assertAccountJsonNode(JsonNode jsonNode) {
      assertThat(jsonNode.isArray()).isTrue();
      assertThat(jsonNode.size()).isEqualTo(2);

      assertThat(jsonNode.get(0).path("id").asLong()).isEqualTo(1L);
      assertThat(jsonNode.get(0).path("accountNumber").asText()).isEqualTo("123456");
      assertThat(jsonNode.get(0).path("owner").asText()).isEqualTo("Juan Perez");
      assertThat(jsonNode.get(0).path("balance").asDouble())
          .isEqualTo(START_AMOUNT_ACCOUNT_1.doubleValue());

      assertThat(jsonNode.get(1).path("id").asLong()).isEqualTo(2L);
      assertThat(jsonNode.get(1).path("accountNumber").asText()).isEqualTo("654321");
      assertThat(jsonNode.get(1).path("owner").asText()).isEqualTo("Maria Lopez");
      assertThat(jsonNode.get(1).path("balance").asDouble())
          .isEqualTo(START_AMOUNT_ACCOUNT_2.doubleValue());
    }

    private void assertAccountList(List<Account> accounts) {
      assertThat(accounts).isNotNull().isNotEmpty().hasSize(2);

      assertThat(accounts.getFirst().getId()).isEqualTo(1L);
      assertThat(accounts.getFirst().getAccountNumber()).isEqualTo("123456");
      assertThat(accounts.getFirst().getOwner()).isEqualTo("Juan Perez");
      assertThat(accounts.getFirst().getBalance().setScale(2, RoundingMode.HALF_UP))
          .isEqualTo(START_AMOUNT_ACCOUNT_1.setScale(2, RoundingMode.HALF_UP));

      assertThat(accounts.getLast().getId()).isEqualTo(2L);
      assertThat(accounts.getLast().getAccountNumber()).isEqualTo("654321");
      assertThat(accounts.getLast().getOwner()).isEqualTo("Maria Lopez");
      assertThat(accounts.getLast().getBalance().setScale(2, RoundingMode.HALF_UP))
          .isEqualTo(START_AMOUNT_ACCOUNT_2.setScale(2, RoundingMode.HALF_UP));
    }
  }

  @Nested
  @Order(3)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class SaveTests {

    @Test
    @Order(1)
    void should_save_an_account() {
      // Given
      Account newAccount = AccountTestDataBuilder.random().build();

      // When
      ResponseEntity<Account> response =
          restTemplateClient.postForEntity(URL_PATH + "/create", newAccount, Account.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
      assertThat(response.getBody()).isNotNull();

      // Testing the saved account
      Account savedAccount = response.getBody();
      assertThat(savedAccount.getId()).isNotNull().isEqualTo(3L);
      assertThat(savedAccount.getAccountNumber()).isEqualTo(newAccount.getAccountNumber());
      assertThat(savedAccount.getOwner()).isEqualTo(newAccount.getOwner());
      assertThat(savedAccount.getBalance()).isEqualTo(newAccount.getBalance());
    }
  }

  @Nested
  @Order(4)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class DeleteTests {

    @Test
    @Order(1)
    void should_delete_an_account() {
      // Given
      Account accountToDelete =
          new Account()
              .setId(1L)
              .setAccountNumber("123456")
              .setOwner("Juan Perez")
              .setBalance(START_AMOUNT_ACCOUNT_1.setScale(2, RoundingMode.HALF_UP));

      // Verifica la cantidad inicial de cuentas
      assertCountAccounts(2);

      // When

      // Option 1: Usa delete con URL pero no retorna nada
      //      restTemplateClient.delete(URL_PATH + "/" + accountToDelete.getId());

      // Option 2: Usa delete con URL y retorna un ResponseEntity
      // el url debe contener el path variable {accountId}
      // el nombre debe ser igual al del controlador
      ResponseEntity<Void> responseDelete =
          restTemplateClient.exchange(
              URL_PATH + "/{accountId}",
              HttpMethod.DELETE,
              // cuerpo de la petición los header, que pueden personalizarse usando HttpHeaders
              null,
              // tipo de retorno
              Void.class,
              // mapa con los valores de las variables de la URL
              Collections.singletonMap("accountId", accountToDelete.getId()));

      // Then

      assertThat(responseDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
      assertThat(responseDelete.getBody()).isNull();

      // Verifica la cantidad de cuentas después de la eliminación
      assertCountAccounts(1);

      // como tenemos implementado el manejo de excepciones en el controlador, no se lanza una
      // excepción 500
      // se lanza una excepción 404
      ResponseEntity<Account> response =
          restTemplateClient.getForEntity(
              URL_PATH + "/" + accountToDelete.getAccountNumber(), Account.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody()).isNull();
    }

    private void assertCountAccounts(int expectedCount) {
      // When
      ResponseEntity<Account[]> response =
          restTemplateClient.getForEntity(URL_PATH, Account[].class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

      assertThat(response.getBody()).isNotNull();

      // Prueba de lista de cuentas
      List<Account> accounts = Arrays.asList(response.getBody());
      assertThat(accounts).isNotNull().isNotEmpty().hasSize(expectedCount);
    }
  }
}
