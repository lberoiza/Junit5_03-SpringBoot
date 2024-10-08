package org.lab.junit5.springboot.testdata;

import lombok.AllArgsConstructor;
import lombok.With;
import net.datafaker.Faker;
import org.lab.junit5.springboot.models.Account;

import java.math.BigDecimal;
import java.util.Locale;

@With
@AllArgsConstructor
public class AccountTestDataBuilder {

  private static final Faker faker = new Faker(Locale.of("es"));

  private Long id = faker.number().randomNumber();
  private String accountNumber = faker.finance().iban();
  private String owner = faker.name().fullName();
  private BigDecimal balance = BigDecimal.valueOf(faker.number().randomDouble(2, 0, 1000));

  private AccountTestDataBuilder() {}

  public static AccountTestDataBuilder random() {
    return new AccountTestDataBuilder();
  }

  public Account build() {
    return new Account()
        .setId(id)
        .setAccountNumber(accountNumber)
        .setOwner(owner)
        .setBalance(balance);
  }
}
