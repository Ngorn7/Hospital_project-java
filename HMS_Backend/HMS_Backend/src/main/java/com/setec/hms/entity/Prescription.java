package com.setec.hms.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "prescriptions")
public class Prescription extends BaseEntity {
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
  @JoinColumn(name = "admission_id")
  public Admission admission;

  @Column(nullable = false)
  public LocalDateTime prescribedAt;

  @Column(columnDefinition = "TEXT")
  public String notes;

  @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
  public List<PrescriptionItem> items = new ArrayList<>();
}
