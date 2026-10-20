package com.setec.hms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true)
  public String name;

  public String location;
  public String phone;
}
