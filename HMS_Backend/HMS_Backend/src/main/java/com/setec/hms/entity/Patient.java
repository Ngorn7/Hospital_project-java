package com.setec.hms.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "patients")
public class Patient extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true)
  public String patientCode;

  @Column(nullable = false)
  public String fullName;

  @Column(nullable = false)
  public LocalDate dateOfBirth;

  public String gender;
  public String phone;
  public String email;

  @Column(columnDefinition = "TEXT")
  public String address;
}
