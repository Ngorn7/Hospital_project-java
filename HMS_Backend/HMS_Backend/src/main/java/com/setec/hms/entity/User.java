package com.setec.hms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true)
  public String username;

  @Column(nullable = false, unique = true)
  public String email;

  @Column(nullable = false)
  public String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Role role;

  @Column(columnDefinition = "bytea")
  public byte[] avatarImage;

  @Column(length = 50)
  public String avatarContentType;

  public boolean enabled = true;
}
