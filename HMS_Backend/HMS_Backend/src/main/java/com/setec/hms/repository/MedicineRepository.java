package com.setec.hms.repository;

import com.setec.hms.entity.*;
import org.springframework.data.jpa.repository.*;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
  long countByStockQuantityLessThan(Integer n);
}
