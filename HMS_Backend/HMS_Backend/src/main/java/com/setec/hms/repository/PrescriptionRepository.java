package com.setec.hms.repository;

import com.setec.hms.entity.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    // Forces the database to fetch the patient, doctor, doctor's department, and
    // medicines all at once
    @Query("SELECT p FROM Prescription p " +
            "LEFT JOIN FETCH p.patient " +
            "LEFT JOIN FETCH p.doctor d " +
            "LEFT JOIN FETCH p.admission " +
            "LEFT JOIN FETCH d.department " +
            "LEFT JOIN FETCH p.items i " +
            "LEFT JOIN FETCH i.medicine " +
            "WHERE p.id = :id")
    Optional<Prescription> findByIdWithDetails(@Param("id") Long id);
}