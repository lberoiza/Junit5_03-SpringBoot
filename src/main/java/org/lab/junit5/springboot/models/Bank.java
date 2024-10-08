package org.lab.junit5.springboot.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class Bank {
  private Long id;
  private String name;
  private int totalOfTransfers;
}
