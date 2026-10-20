package com.setec.hms.controller;

import com.setec.hms.entity.*;
import com.setec.hms.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
  final PatientRepository p;
  final DoctorRepository d;
  final DepartmentRepository dp;
  final AppointmentRepository a;
  final AdmissionRepository ad;
  final RoomRepository r;
  final BillRepository b;
  final MedicineRepository m;

  public DashboardController(
      PatientRepository p,
      DoctorRepository d,
      DepartmentRepository dp,
      AppointmentRepository a,
      AdmissionRepository ad,
      RoomRepository r,
      BillRepository b,
      MedicineRepository m) {
    this.p = p;
    this.d = d;
    this.dp = dp;
    this.a = a;
    this.ad = ad;
    this.r = r;
    this.b = b;
    this.m = m;
  }

  @GetMapping
  Map<String, Long> summary() {
    LocalDateTime start = LocalDate.now().atStartOfDay();
    return Map.of(
        "totalPatients",
        p.count(),
        "totalDoctors",
        d.count(),
        "totalDepartments",
        dp.count(),
        "todaysAppointments",
        a.countByAppointmentAtBetween(start, start.plusDays(1)),
        "currentlyAdmitted",
        ad.countByStatus(AdmissionStatus.ADMITTED),
        "availableRooms",
        r.countByStatus(RoomStatus.AVAILABLE),
        "unpaidBills",
        b.countByStatus(BillStatus.UNPAID),
        "lowStockMedicines",
        m.countByStockQuantityLessThan(10));
  }
}
