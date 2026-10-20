package com.setec.hms.repository;

import com.setec.hms.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface RoomRepository extends JpaRepository<Room, Long> {
  List<Room> findByStatus(RoomStatus s);

  long countByStatus(RoomStatus s);

  // ADD THIS: Forces the database to load the Department alongside the Room
  @Query("SELECT r FROM Room r LEFT JOIN FETCH r.department")
  List<Room> findAllWithDepartment();
}