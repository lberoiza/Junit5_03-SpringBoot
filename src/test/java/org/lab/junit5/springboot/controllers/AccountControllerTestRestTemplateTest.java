package org.lab.junit5.springboot.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.lab.junit5.springboot.models.dtos.TransferDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/testdata/data-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(
    scripts = "/testdata/data-test-cleaner.sql",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AccountControllerTestRestTemplateTest {

  private static final String URL_PATH = "/api/accounts";
  private static ObjectMapper objectMapper = new ObjectMapper();

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
  class Transfer {

    @Test
    @Order(1)
    void test_as_map() {
      // Given
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, BigDecimal.TEN);

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
      softly.assertThat(((Map) response.getBody().get("data")).get("amount")).isEqualTo(10);

      softly.assertAll();
    }

    @Test
    @Order(2)
    void test_as_json() throws JsonProcessingException {
      // Given
      TransferDetailDTO transferDetailDTO = new TransferDetailDTO(1L, 2L, 1L, BigDecimal.TEN);

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
      assertThat(jsonNode.path("data").path("amount").asDouble()).isEqualTo(10);
    }

    @Test
    @Order(3)
    void expected_error_Insufficient_money() {
      // Given
      TransferDetailDTO transferDetailDTO =
          new TransferDetailDTO(1L, 2L, 1L, BigDecimal.valueOf(9999.00));

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
                  .formatted("123456", 1000.00, 9999.00));
    }
  }

  @Nested
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class Details {}
}
