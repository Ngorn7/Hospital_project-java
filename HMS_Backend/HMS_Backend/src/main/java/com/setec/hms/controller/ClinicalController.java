package com.setec.hms.controller;

import com.setec.hms.entity.*;
import com.setec.hms.repository.*;
import com.setec.hms.service.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.*;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Transactional
public class ClinicalController {
  final HmsService s;
  final AppointmentRepository ar;
  final AdmissionRepository adr;
  final PrescriptionRepository pr;
  final BillRepository br;

  final PatientRepository patientsRepo;
  final DoctorRepository doctorsRepo;
  final RoomRepository roomsRepo;
  final EntityManager entityManager;

  public ClinicalController(
      HmsService x,
      AppointmentRepository a,
      AdmissionRepository ad,
      PrescriptionRepository p,
      BillRepository b,
      PatientRepository pat,
      DoctorRepository doc,
      RoomRepository r,
      EntityManager em) {
    s = x;
    ar = a;
    adr = ad;
    pr = p;
    br = b;
    patientsRepo = pat;
    doctorsRepo = doc;
    roomsRepo = r;
    entityManager = em;
  }

  record AppointmentReq(
      @NotNull Long patientId,
      @NotNull Long doctorId,
      @NotNull LocalDateTime appointmentAt,
      String reason) {
  }

  record AdmissionReq(
      @NotNull Long patientId, @NotNull Long doctorId, @NotNull Long roomId, String diagnosis) {
  }

  record PrescriptionReq(
      @NotNull Long patientId,
      @NotNull Long doctorId,
      Long admissionId,
      String notes,
      @NotEmpty List<HmsService.@Valid Item> items) {
  }

  record BillReq(
      @NotNull Long patientId,
      Long appointmentId,
      Long admissionId,
      @DecimalMin("0.0") BigDecimal additionalCharges) {
  }

  record PatientView(Long id, String patientCode, String fullName) {
  }

  record DepartmentView(Long id, String name) {
  }

  record DoctorView(Long id, String fullName, String specialty, DepartmentView department) {
  }

  record RoomView(Long id, String roomNumber, String roomType, RoomStatus status) {
  }

  record AppointmentView(
      Long id,
      PatientView patient,
      DoctorView doctor,
      LocalDateTime appointmentAt,
      AppointmentStatus status,
      String reason,
      LocalDateTime createdAt) {
  }

  record AdmissionView(
      Long id,
      PatientView patient,
      DoctorView doctor,
      RoomView room,
      LocalDateTime admittedAt,
      LocalDateTime dischargedAt,
      String diagnosis,
      AdmissionStatus status,
      LocalDateTime createdAt) {
  }

  record MedicineView(Long id, String name, String unit, BigDecimal unitPrice) {
  }

  record PrescriptionItemView(
      Long id,
      MedicineView medicine,
      String dosage,
      String frequency,
      Integer durationDays,
      Integer quantity) {
  }

  record PrescriptionView(
      Long id,
      PatientView patient,
      DoctorView doctor,
      Long admissionId,
      LocalDateTime prescribedAt,
      String notes,
      List<PrescriptionItemView> items,
      LocalDateTime createdAt) {
  }

  record AppointmentBillView(Long id, LocalDateTime appointmentAt, DoctorView doctor) {
  }

  record AdmissionBillView(
      Long id, LocalDateTime admittedAt, LocalDateTime dischargedAt, RoomView room) {
  }

  record BillView(
      Long id,
      PatientView patient,
      AppointmentBillView appointment,
      AdmissionBillView admission,
      LocalDate issuedAt,
      BigDecimal totalAmount,
      BillStatus status,
      LocalDateTime paidAt,
      LocalDateTime createdAt) {
  }

  private PatientView toPatientView(Patient patient) {
    return new PatientView(patient.id, patient.patientCode, patient.fullName);
  }

  private DoctorView toDoctorView(Doctor doctor) {
    return new DoctorView(
        doctor.id,
        doctor.fullName,
        doctor.specialty,
        new DepartmentView(doctor.department.id, doctor.department.name));
  }

  private RoomView toRoomView(Room room) {
    return new RoomView(room.id, room.roomNumber, room.roomType, room.status);
  }

  private AppointmentView toAppointmentView(Appointment appointment) {
    return new AppointmentView(
        appointment.id,
        toPatientView(appointment.patient),
        toDoctorView(appointment.doctor),
        appointment.appointmentAt,
        appointment.status,
        appointment.reason,
        appointment.createdAt);
  }

  private AdmissionView toAdmissionView(Admission admission) {
    return new AdmissionView(
        admission.id,
        toPatientView(admission.patient),
        toDoctorView(admission.doctor),
        toRoomView(admission.room),
        admission.admittedAt,
        admission.dischargedAt,
        admission.diagnosis,
        admission.status,
        admission.createdAt);
  }

  private PrescriptionView toPrescriptionView(Prescription prescription) {
    List<PrescriptionItemView> items = prescription.items.stream()
        .map(
            item -> new PrescriptionItemView(
                item.id,
                new MedicineView(
                    item.medicine.id,
                    item.medicine.name,
                    item.medicine.unit,
                    item.medicine.unitPrice),
                item.dosage,
                item.frequency,
                item.durationDays,
                item.quantity))
        .toList();
    return new PrescriptionView(
        prescription.id,
        toPatientView(prescription.patient),
        toDoctorView(prescription.doctor),
        prescription.admission == null ? null : prescription.admission.id,
        prescription.prescribedAt,
        prescription.notes,
        items,
        prescription.createdAt);
  }

  private BillView toBillView(Bill bill) {
    AppointmentBillView appointment = bill.appointment == null
        ? null
        : new AppointmentBillView(
            bill.appointment.id,
            bill.appointment.appointmentAt,
            toDoctorView(bill.appointment.doctor));
    AdmissionBillView admission = bill.admission == null
        ? null
        : new AdmissionBillView(
            bill.admission.id,
            bill.admission.admittedAt,
            bill.admission.dischargedAt,
            toRoomView(bill.admission.room));
    return new BillView(
        bill.id,
        toPatientView(bill.patient),
        appointment,
        admission,
        bill.issuedAt,
        bill.totalAmount,
        bill.status,
        bill.paidAt,
        bill.createdAt);
  }

  // --- APPOINTMENTS ---

  @PostMapping("/appointments")
  ResponseEntity<AppointmentView> appointment(@Valid @RequestBody AppointmentReq r) {
    return ResponseEntity.status(201)
        .body(toAppointmentView(s.appointment(r.patientId, r.doctorId, r.appointmentAt, r.reason)));
  }

  // FIXED: Using explicit @RequestParam for Swagger
  @GetMapping("/appointments")
  Page<AppointmentView> appointments(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String sort) {
    return ar.findAllWithDetails(Paging.of(page, size, sort, "appointmentAt,desc"))
        .map(this::toAppointmentView);
  }

  @PutMapping("/appointments/{id}")
  ResponseEntity<AppointmentView> appointmentUpdate(
      @PathVariable Long id, @Valid @RequestBody AppointmentReq r) {
    s.updateAppointment(id, r.patientId, r.doctorId, r.appointmentAt, r.reason);
    ar.flush();
    entityManager.clear();
    return ResponseEntity.ok(toAppointmentView(s.req(ar.findByIdWithDetails(id), "Appointment")));
  }

  @DeleteMapping("/appointments/{id}")
  ResponseEntity<Void> appointmentDelete(@PathVariable Long id) {
    ar.delete(s.req(ar.findById(id), "Appointment"));
    ar.flush();
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/appointments/{id}/status")
  AppointmentView appStatus(@PathVariable Long id, @RequestParam AppointmentStatus status) {
    s.transition(id, status);
    ar.flush();
    entityManager.clear();
    return toAppointmentView(s.req(ar.findByIdWithDetails(id), "Appointment"));
  }

  // --- ADMISSIONS ---

  @PostMapping("/admissions")
  ResponseEntity<AdmissionView> admit(@Valid @RequestBody AdmissionReq r) {
    return ResponseEntity.status(201)
        .body(toAdmissionView(s.admit(r.patientId, r.doctorId, r.roomId, r.diagnosis)));
  }

  @GetMapping("/admissions")
  Page<AdmissionView> admissions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String sort) {
    return adr.findAllWithDetails(Paging.of(page, size, sort, "admittedAt,desc"))
        .map(this::toAdmissionView);
  }

  @PutMapping("/admissions/{id}")
  ResponseEntity<AdmissionView> admissionUpdate(
      @PathVariable Long id, @Valid @RequestBody AdmissionReq r) {
    s.updateAdmission(id, r.patientId, r.doctorId, r.roomId, r.diagnosis);
    adr.flush();
    entityManager.clear();
    return ResponseEntity.ok(toAdmissionView(s.req(adr.findByIdWithDetails(id), "Admission")));
  }

  @DeleteMapping("/admissions/{id}")
  ResponseEntity<Void> admissionDelete(@PathVariable Long id) {
    s.removeAdmission(id);
    adr.flush();
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/admissions/{id}/discharge")
  AdmissionView discharge(@PathVariable Long id) {
    s.discharge(id);
    adr.flush();
    entityManager.clear();
    return toAdmissionView(s.req(adr.findByIdWithDetails(id), "Admission"));
  }

  // --- PRESCRIPTIONS ---

  @PostMapping("/prescriptions")
  ResponseEntity<PrescriptionView> prescription(@Valid @RequestBody PrescriptionReq r) {
    return ResponseEntity.status(201)
        .body(toPrescriptionView(s.prescribe(r.patientId, r.doctorId, r.admissionId, r.notes, r.items)));
  }

  @GetMapping("/prescriptions/{id}")
  PrescriptionView prescription(@PathVariable Long id) {
    return toPrescriptionView(s.req(pr.findByIdWithDetails(id), "Prescription"));
  }

  // --- BILLING ---

  @PostMapping("/bills")
  ResponseEntity<BillView> bill(@Valid @RequestBody BillReq r) {
    return ResponseEntity.status(201)
        .body(toBillView(s.bill(r.patientId, r.appointmentId, r.admissionId, r.additionalCharges)));
  }

  // FIXED: Using explicit @RequestParam for Swagger
  @GetMapping("/bills")
  Page<BillView> bills(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String sort) {
    return br.findAllWithDetails(Paging.of(page, size, sort, "issuedAt,desc"))
        .map(this::toBillView);
  }

  @PatchMapping("/bills/{id}/pay")
  BillView pay(@PathVariable Long id) {
    s.pay(id);
    entityManager.clear();
    Bill updatedBill = s.req(br.findByIdWithDetails(id), "Bill");
    return toBillView(updatedBill);
  }

  @DeleteMapping("/bills/{id}")
  ResponseEntity<Void> billDelete(@PathVariable Long id) {
    br.delete(s.req(br.findById(id), "Bill"));
    br.flush();
    return ResponseEntity.noContent().build();
  }
}