package com.setec.hms.entity;

import jakarta.persistence.*;
import java.math.*;

@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false)
  public Department department;

  @Column(nullable = false, unique = true)
  public String roomNumber;

  @Column(nullable = false)
  public String roomType;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal dailyRate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public RoomStatus status = RoomStatus.AVAILABLE;
}
