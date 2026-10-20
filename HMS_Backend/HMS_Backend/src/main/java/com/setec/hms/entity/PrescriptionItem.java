package com.setec.hms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "prescription_items")
public class PrescriptionItem extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prescription_id", nullable = false)
  @JsonIgnore
  public Prescription prescription;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "medicine_id", nullable = false)
  public Medicine medicine;

  @Column(nullable = false)
  public String dosage;

  @Column(nullable = false)
  public String frequency;

  @Column(nullable = false)
  public Integer durationDays;

  @Column(nullable = false)
  public Integer quantity;
}
