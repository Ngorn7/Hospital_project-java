package com.setec.hms.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "admissions")
public class Admission extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  public Patient patient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", nullable = false)
  public Doctor doctor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  public Room room;

  @Column(nullable = false)
  public LocalDateTime admittedAt;

  public LocalDateTime dischargedAt;

  @Column(columnDefinition = "TEXT")
  public String diagnosis;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public AdmissionStatus status = AdmissionStatus.ADMITTED;
}
