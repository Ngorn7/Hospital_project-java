package com.setec.hms.repository;

import com.setec.hms.entity.*;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {
  boolean existsByRoomIdAndStatus(Long id, AdmissionStatus s);

  long countByStatus(AdmissionStatus s);

  @Query(value = "SELECT a FROM Admission a " +
      "LEFT JOIN FETCH a.patient " +
      "LEFT JOIN FETCH a.doctor d " +
      "LEFT JOIN FETCH d.department " +
      "LEFT JOIN FETCH a.room", countQuery = "SELECT count(a) FROM Admission a")
  Page<Admission> findAllWithDetails(Pageable pageable);

  // Add this to fetch a single admission with ALL details loaded
  @Query("SELECT a FROM Admission a " +
      "LEFT JOIN FETCH a.patient " +
      "LEFT JOIN FETCH a.doctor d " +
      "LEFT JOIN FETCH d.department " +
      "LEFT JOIN FETCH a.room " +
      "WHERE a.id = :id")
  Optional<Admission> findByIdWithDetails(@Param("id") Long id);
}