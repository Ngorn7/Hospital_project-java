package com.setec.hms.repository;

import com.setec.hms.entity.*;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BillRepository extends JpaRepository<Bill, Long> {
  boolean existsByAppointmentId(Long id);

  boolean existsByAdmissionId(Long id);

  long countByStatus(BillStatus s);

  @Query(value = "SELECT b FROM Bill b " +
      "LEFT JOIN FETCH b.patient " +
      "LEFT JOIN FETCH b.appointment a " +
      "LEFT JOIN FETCH a.doctor d " +
      "LEFT JOIN FETCH d.department " +
      "LEFT JOIN FETCH b.admission ad " +
      "LEFT JOIN FETCH ad.room", countQuery = "SELECT count(b) FROM Bill b")
  Page<Bill> findAllWithDetails(Pageable pageable);

  // ADD THIS METHOD
  @Query("SELECT b FROM Bill b " +
      "LEFT JOIN FETCH b.patient " +
      "LEFT JOIN FETCH b.appointment a " +
      "LEFT JOIN FETCH a.doctor d " +
      "LEFT JOIN FETCH d.department " +
      "LEFT JOIN FETCH b.admission ad " +
      "LEFT JOIN FETCH ad.room " +
      "WHERE b.id = :id")
  Optional<Bill> findByIdWithDetails(@Param("id") Long id);
}