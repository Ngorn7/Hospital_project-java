package com.setec.hms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.setec.hms.entity.*;
import com.setec.hms.exception.*;
import com.setec.hms.repository.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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
