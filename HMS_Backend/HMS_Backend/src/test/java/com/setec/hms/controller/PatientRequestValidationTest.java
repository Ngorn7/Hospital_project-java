package com.setec.hms.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.setec.hms.entity.PatientGender;
import org.junit.jupiter.api.Test;

class PatientRequestValidationTest {

  @Test
  void genderValuesMatchDatabaseCheckConstraint() {
    assertArrayEquals(
        new PatientGender[] {PatientGender.MALE, PatientGender.FEMALE, PatientGender.OTHER},
        PatientGender.values());
    assertThrows(IllegalArgumentException.class, () -> PatientGender.valueOf("Male"));
    assertThrows(IllegalArgumentException.class, () -> PatientGender.valueOf("Make"));
  }
}
