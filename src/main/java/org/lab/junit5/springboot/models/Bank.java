package org.lab.junit5.springboot.models;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Bank {
  private Long id;
  private String name;
  private int totalOfTransactions;
}
