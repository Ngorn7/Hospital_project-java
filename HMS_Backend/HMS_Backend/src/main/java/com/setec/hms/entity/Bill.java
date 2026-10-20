package com.setec.hms.entity;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;

@Entity
@Table(name = "billing")
public class Bill extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  public Patient patient;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id")
  public Appointment appointment;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "admission_id")
  public Admission admission;

  @Column(nullable = false)
  public LocalDate issuedAt;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public BillStatus status = BillStatus.UNPAID;

  public LocalDateTime paidAt;
}
