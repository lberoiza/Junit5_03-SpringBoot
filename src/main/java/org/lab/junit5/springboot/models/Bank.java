package org.lab.junit5.springboot.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "banks")
public class Bank {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
  private int totalOfTransactions;
}
