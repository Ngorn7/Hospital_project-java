package com.setec.hms.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  public Patient patient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", nullable = false)
  public Doctor doctor;

  @Column(nullable = false)
  public LocalDateTime appointmentAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public AppointmentStatus status = AppointmentStatus.SCHEDULED;

  @Column(columnDefinition = "TEXT")
  public String reason;
}
