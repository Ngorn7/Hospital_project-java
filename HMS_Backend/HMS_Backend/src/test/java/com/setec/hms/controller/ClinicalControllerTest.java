package com.setec.hms.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.setec.hms.entity.Appointment;
import com.setec.hms.entity.Department;
import com.setec.hms.entity.Doctor;
import com.setec.hms.entity.Patient;
import com.setec.hms.repository.AdmissionRepository;
import com.setec.hms.repository.AppointmentRepository;
import com.setec.hms.repository.BillRepository;
import com.setec.hms.repository.DoctorRepository;
import com.setec.hms.repository.PatientRepository;
import com.setec.hms.repository.PrescriptionRepository;
import com.setec.hms.repository.RoomRepository;
import com.setec.hms.service.HmsService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

class ClinicalControllerTest {

  @Test
  void appointmentResponseIncludesRelatedDisplayNames() {
    AppointmentRepository appointments = mock(AppointmentRepository.class);
    Patient patient = new Patient();
    patient.id = 12L;
    patient.patientCode = "P-0012";
    patient.fullName = "Barry Allen";
    Department department = new Department();
    department.id = 3L;
    department.name = "Cardiology";
    Doctor doctor = new Doctor();
    doctor.id = 8L;
    doctor.fullName = "Dr. Oliver Queen";
    doctor.specialty = "Cardiology";
    doctor.department = department;
    Appointment appointment = new Appointment();
    appointment.id = 44L;
    appointment.patient = patient;
    appointment.doctor = doctor;
    appointment.appointmentAt = LocalDateTime.of(2026, 10, 20, 9, 30);

    when(appointments.findAllWithDetails(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(appointment)));

    ClinicalController controller =
        new ClinicalController(
            mock(HmsService.class),
            appointments,
            mock(AdmissionRepository.class),
            mock(PrescriptionRepository.class),
            mock(BillRepository.class),
            mock(PatientRepository.class),
            mock(DoctorRepository.class),
            mock(RoomRepository.class),
            mock(EntityManager.class));

    ClinicalController.AppointmentView result =
        controller.appointments(0, 10, "appointmentAt,desc").getContent().get(0);

    assertEquals("Barry Allen", result.patient().fullName());
    assertEquals("Dr. Oliver Queen", result.doctor().fullName());
    assertEquals("Cardiology", result.doctor().department().name());
  }
}
