package com.setec.hms.entity;

import jakarta.persistence.*;
import java.math.*;

@Entity
@Table(name = "medicines")
public class Medicine extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true)
  public String name;

  public String unit;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal unitPrice;

  @Column(nullable = false)
  public Integer stockQuantity;
}
