package com.setec.hms.repository;

import com.setec.hms.entity.*;
import java.time.*;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
  boolean existsByDoctorIdAndAppointmentAtAndStatusNot(
      Long d, LocalDateTime t, AppointmentStatus s);

  long countByAppointmentAtBetween(LocalDateTime a, LocalDateTime b);

  boolean existsByDoctorIdAndAppointmentAtAndStatusNotAndIdNot(
      Long d, LocalDateTime t, AppointmentStatus s, Long id);

  @Query(value = "SELECT a FROM Appointment a " +
      "LEFT JOIN FETCH a.patient " +
      "LEFT JOIN FETCH a.doctor d " +
      "LEFT JOIN FETCH d.department", countQuery = "SELECT count(a) FROM Appointment a")
  Page<Appointment> findAllWithDetails(Pageable pageable);

  @Query("SELECT a FROM Appointment a " +
      "LEFT JOIN FETCH a.patient " +
      "LEFT JOIN FETCH a.doctor d " +
      "LEFT JOIN FETCH d.department " +
      "WHERE a.id = :id")
  Optional<Appointment> findByIdWithDetails(@Param("id") Long id);
}