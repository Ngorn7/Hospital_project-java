package com.setec.hms.entity;

import jakarta.persistence.*;
import java.time.*;

@MappedSuperclass
public abstract class BaseEntity {
  @Column(nullable = false, updatable = false)
  public LocalDateTime createdAt;

  @PrePersist
  void created() {
    createdAt = LocalDateTime.now();
  }
}
