package com.setec.hms.repository;

import com.setec.hms.entity.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

  List<Doctor> findByDepartmentIdAndSpecialtyContainingIgnoreCase(Long d, String s);

  // ADD THIS METHOD: Forces the database to load the Department alongside the
  // Doctor
  @Query(value = "SELECT d FROM Doctor d LEFT JOIN FETCH d.department", countQuery = "SELECT count(d) FROM Doctor d")
  Page<Doctor> findAllWithDepartment(Pageable pageable);
}