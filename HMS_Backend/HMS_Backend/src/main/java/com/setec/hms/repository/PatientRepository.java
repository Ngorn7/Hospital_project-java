package com.setec.hms.repository;

import com.setec.hms.entity.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface PatientRepository extends JpaRepository<Patient, Long> {
  boolean existsByPatientCode(String c);

  Page<Patient> findByFullNameContainingIgnoreCaseOrPatientCodeContainingIgnoreCaseOrPhoneContaining(
      String name, String code, String phone, Pageable pageable);
}
