package org.lab.junit5.springboot.config;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

@Profile("test")
@Configuration
@Slf4j
@RequiredArgsConstructor
public class ProfileTestConfiguration {

  private final DataSource dataSource;

  private final ResourcePatternResolver resourcePatternResolver;

  @PostConstruct
  public void loadTestData() {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // Cargar todos los archivos .sql en el directorio "testdata"
      Resource[] resources = resourcePatternResolver.getResources("classpath:testdata/*.sql");

      for (Resource resource : resources) {
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
          // Leer y ejecutar el contenido de cada archivo SQL
          String sqlScript = reader.lines().collect(Collectors.joining("\n"));
          statement.execute(sqlScript);

          log.info("Ejecutado archivo SQL: {}", resource.getFilename());
        } catch (Exception e) {
          log.error("Error al ejecutar '{}': {}", resource.getFilename(), e.getMessage());
        }
      }

      log.info("Todos los archivos SQL en 'testdata' fueron ejecutados exitosamente.");

    } catch (Exception e) {
      log.error(
          "Error al cargar datos de prueba desde los archivos en 'testdata': {}", e.getMessage());
    }
  }
}
