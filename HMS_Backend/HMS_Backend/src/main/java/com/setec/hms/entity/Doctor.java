package com.setec.hms.entity;

import jakarta.persistence.*;
import java.math.*;

@Entity
@Table(name = "doctors")
public class Doctor extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false)
  public Department department;

  @Column(nullable = false)
  public String fullName;

  public String specialty;
  public String phone;

  @Column(unique = true)
  public String email;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal consultationFee = BigDecimal.ZERO;
}
