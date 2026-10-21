package com.setec.hms.service;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.setec.hms.entity.Appointment;
import com.setec.hms.entity.AppointmentStatus;
import com.setec.hms.entity.Room;
import com.setec.hms.entity.RoomStatus;
import com.setec.hms.exception.ConflictException;
import com.setec.hms.repository.AdmissionRepository;
import com.setec.hms.repository.AppointmentRepository;
import com.setec.hms.repository.BillRepository;
import com.setec.hms.repository.DepartmentRepository;
import com.setec.hms.repository.DoctorRepository;
import com.setec.hms.repository.MedicineRepository;
import com.setec.hms.repository.PatientRepository;
import com.setec.hms.repository.PrescriptionRepository;
import com.setec.hms.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class HmsServiceTest {
  @Mock PatientRepository patients;
  @Mock DoctorRepository doctors;
  @Mock DepartmentRepository departments;
  @Mock RoomRepository rooms;
  @Mock AppointmentRepository appointments;
  @Mock AdmissionRepository admissions;
  @Mock MedicineRepository medicines;
  @Mock PrescriptionRepository prescriptions;
  @Mock BillRepository bills;
  HmsService service;

  @BeforeEach
  void setup() {
    service =
        new HmsService(
            patients,
            doctors,
            departments,
            rooms,
            appointments,
            admissions,
            medicines,
            prescriptions,
            bills);
  }

  @Test
  void rejectsDoctorAppointmentConflict() {
    when(appointments.existsByDoctorIdAndAppointmentAtAndStatusNot(
            eq(2L), any(), eq(AppointmentStatus.CANCELLED)))
        .thenReturn(true);
    assertThrows(
        ConflictException.class, () -> service.appointment(1L, 2L, LocalDateTime.now(), "checkup"));
  }

  @Test
  void rejectsSkippingAppointmentStates() {
    Appointment a = new Appointment();
    a.status = AppointmentStatus.SCHEDULED;
    when(appointments.findById(5L)).thenReturn(java.util.Optional.of(a));
    assertThrows(ConflictException.class, () -> service.transition(5L, AppointmentStatus.COMPLETED));
    assertEquals(AppointmentStatus.SCHEDULED, a.status);
  }

  @Test
  void confirmsScheduledAppointment() {
    Appointment a = new Appointment();
    a.status = AppointmentStatus.SCHEDULED;
    when(appointments.findById(5L)).thenReturn(java.util.Optional.of(a));
    assertEquals(AppointmentStatus.CONFIRMED, service.transition(5L, AppointmentStatus.CONFIRMED).status);
  }

  @Test
  void rejectsOccupiedRoom() {
    Room r = new Room();
    r.status = RoomStatus.OCCUPIED;
    when(rooms.findById(1L)).thenReturn(java.util.Optional.of(r));
    assertThrows(ConflictException.class, () -> service.admit(2L, 3L, 1L, "recorded diagnosis"));
  }
}
