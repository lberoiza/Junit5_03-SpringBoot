package org.lab.junit5.springboot.testdata;

import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.With;
import net.datafaker.Faker;
import org.lab.junit5.springboot.models.Bank;

@With
@AllArgsConstructor
public class BankTestDataBuilder {

  private static final Faker faker = new Faker(Locale.of("es"));

  private Long id = faker.number().randomNumber();
  private String name = "Bank: %s".formatted(faker.finance().creditCard());
  private String owner = faker.name().fullName();
  private int totalOfTransfers =faker.number().numberBetween(0, 100);

  private BankTestDataBuilder() {}

  public static BankTestDataBuilder random() {
    return new BankTestDataBuilder();
  }

  public Bank build() {
    return new Bank()
        .setId(id)
        .setName(name)
        .setTotalOfTransfers(totalOfTransfers);
  }
}
