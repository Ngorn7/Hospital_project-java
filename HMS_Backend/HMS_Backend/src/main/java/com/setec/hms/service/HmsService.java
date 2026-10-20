package com.setec.hms.service;

import com.setec.hms.entity.*;
import com.setec.hms.exception.*;
import com.setec.hms.repository.*;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

@Service
@Transactional
public class HmsService {
  final PatientRepository patients;
  final DoctorRepository doctors;
  final DepartmentRepository departments;
  final RoomRepository rooms;
  final AppointmentRepository appointments;
  final AdmissionRepository admissions;
  final MedicineRepository medicines;
  final PrescriptionRepository prescriptions;
  final BillRepository bills;

  public HmsService(
      PatientRepository p,
      DoctorRepository d,
      DepartmentRepository dp,
      RoomRepository r,
      AppointmentRepository a,
      AdmissionRepository ad,
      MedicineRepository m,
      PrescriptionRepository pr,
      BillRepository b) {
    patients = p;
    doctors = d;
    departments = dp;
    rooms = r;
    appointments = a;
    admissions = ad;
    medicines = m;
    prescriptions = pr;
    bills = b;
  }

  public <T> T req(Optional<T> x, String n) {
    return x.orElseThrow(() -> new NotFoundException(n + " not found"));
  }

  // --- PATIENTS ---

  public Patient patient(Patient x) {
    if (x.id == null) {
      x.patientCode = "P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    return patients.save(x);
  }

  /** Paged search. The previous version ran the query twice and returned every match as one page. */
  public Page<Patient> patientList(String q, Pageable p) {
    if (q == null || q.isBlank()) return patients.findAll(p);
    String term = q.trim();
    return patients.findByFullNameContainingIgnoreCaseOrPatientCodeContainingIgnoreCaseOrPhoneContaining(
        term, term, term, p);
  }

  // --- APPOINTMENTS ---

  public Appointment appointment(Long patientId, Long doctorId, LocalDateTime at, String reason) {
    if (appointments.existsByDoctorIdAndAppointmentAtAndStatusNot(
        doctorId, at, AppointmentStatus.CANCELLED))
      throw new ConflictException("Doctor already has an appointment at this time");
    Appointment a = new Appointment();
    a.patient = req(patients.findById(patientId), "Patient");
    a.doctor = req(doctors.findById(doctorId), "Doctor");
    a.appointmentAt = at;
    a.reason = reason;
    return appointments.save(a);
  }

  public Appointment updateAppointment(
      Long id, Long patientId, Long doctorId, LocalDateTime at, String reason) {
    Appointment a = req(appointments.findById(id), "Appointment");
    if (appointments.existsByDoctorIdAndAppointmentAtAndStatusNotAndIdNot(
        doctorId, at, AppointmentStatus.CANCELLED, id))
      throw new ConflictException("Doctor already has an appointment at this time");
    a.patient = req(patients.findById(patientId), "Patient");
    a.doctor = req(doctors.findById(doctorId), "Doctor");
    a.appointmentAt = at;
    a.reason = reason;
    return a;
  }

  /** The only place appointment status changes are allowed. Callers must not set status directly. */
  public Appointment transition(Long id, AppointmentStatus next) {
    Appointment a = req(appointments.findById(id), "Appointment");
    boolean ok =
        switch (a.status) {
          case SCHEDULED ->
              next == AppointmentStatus.CONFIRMED || next == AppointmentStatus.CANCELLED;
          case CONFIRMED ->
              next == AppointmentStatus.COMPLETED || next == AppointmentStatus.CANCELLED;
          default -> false;
        };
    if (!ok) throw new ConflictException("Invalid appointment status transition");
    a.status = next;
    return a;
  }

  // --- ROOMS ---

  /**
   * Room status is driven by admissions. A room with an active admission must stay OCCUPIED, and a
   * room can only become OCCUPIED through an admission.
   */
  public Room applyRoomStatus(Room room, RoomStatus requested) {
    RoomStatus next = requested == null ? room.status : requested;
    boolean admitted =
        room.id != null && admissions.existsByRoomIdAndStatus(room.id, AdmissionStatus.ADMITTED);
    if (admitted && next != RoomStatus.OCCUPIED)
      throw new ConflictException(
          "Room has an active admission and must stay occupied until the patient is discharged");
    if (!admitted && next == RoomStatus.OCCUPIED)
      throw new ConflictException("A room becomes occupied only through an admission");
    room.status = next;
    return room;
  }

  // --- ADMISSIONS ---

  public Admission admit(Long patientId, Long doctorId, Long roomId, String diagnosis) {
    Room r = req(rooms.findById(roomId), "Room");
    if (r.status != RoomStatus.AVAILABLE
        || admissions.existsByRoomIdAndStatus(roomId, AdmissionStatus.ADMITTED))
      throw new ConflictException("Room is not available");
    Admission a = new Admission();
    a.patient = req(patients.findById(patientId), "Patient");
    a.doctor = req(doctors.findById(doctorId), "Doctor");
    a.room = r;
    a.admittedAt = LocalDateTime.now();
    a.diagnosis = diagnosis;
    r.status = RoomStatus.OCCUPIED;
    return admissions.save(a);
  }

  public Admission updateAdmission(
      Long id, Long patientId, Long doctorId, Long roomId, String diagnosis) {
    Admission a = req(admissions.findByIdWithDetails(id), "Admission");
    Room target = req(rooms.findById(roomId), "Room");
    if (!a.room.id.equals(roomId)) {
      if (a.status != AdmissionStatus.ADMITTED)
        throw new ConflictException("Only an active admission can be moved to another room");
      if (target.status != RoomStatus.AVAILABLE
          || admissions.existsByRoomIdAndStatus(roomId, AdmissionStatus.ADMITTED))
        throw new ConflictException("Room is not available");
      a.room.status = RoomStatus.AVAILABLE;
      a.room = target;
      target.status = RoomStatus.OCCUPIED;
    }
    a.patient = req(patients.findById(patientId), "Patient");
    a.doctor = req(doctors.findById(doctorId), "Doctor");
    a.diagnosis = diagnosis;
    return a;
  }

  public Admission discharge(Long id) {
    Admission a = req(admissions.findByIdWithDetails(id), "Admission");
    if (a.status != AdmissionStatus.ADMITTED)
      throw new ConflictException("Admission is already discharged");
    a.status = AdmissionStatus.DISCHARGED;
    a.dischargedAt = LocalDateTime.now();
    a.room.status = RoomStatus.AVAILABLE;
    return a;
  }

  /** Deleting an active admission must release its room, or the room stays occupied forever. */
  public void removeAdmission(Long id) {
    Admission a = req(admissions.findByIdWithDetails(id), "Admission");
    if (a.status == AdmissionStatus.ADMITTED) a.room.status = RoomStatus.AVAILABLE;
    admissions.delete(a);
  }

  // --- PRESCRIPTIONS ---

  public Prescription prescribe(Long p, Long d, Long admissionId, String notes, List<Item> xs) {
    Prescription x = new Prescription();
    x.patient = req(patients.findById(p), "Patient");
    x.doctor = req(doctors.findById(d), "Doctor");
    if (admissionId != null)
      x.admission = req(admissions.findById(admissionId), "Admission");
    x.prescribedAt = LocalDateTime.now();
    x.notes = notes;
    for (Item i : xs) {
      Medicine m = req(medicines.findById(i.medicineId), "Medicine");
      if (m.stockQuantity < i.quantity) throw new ConflictException("Insufficient medicine stock");
      m.stockQuantity -= i.quantity;
      PrescriptionItem pi = new PrescriptionItem();
      pi.prescription = x;
      pi.medicine = m;
      pi.dosage = i.dosage;
      pi.frequency = i.frequency;
      pi.durationDays = i.durationDays;
      pi.quantity = i.quantity;
      x.items.add(pi);
    }
    return prescriptions.save(x);
  }

  public record Item(
      @NotNull Long medicineId,
      @NotBlank String dosage,
      @NotBlank String frequency,
      @NotNull @Min(1) Integer durationDays,
      @NotNull @Min(1) Integer quantity) {}

  // --- BILLING ---

  public Bill bill(Long patientId, Long appointmentId, Long admissionId, BigDecimal extra) {
    if ((appointmentId == null) == (admissionId == null))
      throw new IllegalArgumentException("Exactly one appointment or admission is required");
    Bill b = new Bill();
    b.patient = req(patients.findById(patientId), "Patient");
    b.issuedAt = LocalDate.now();
    BigDecimal charges = extra == null ? BigDecimal.ZERO : extra;
    if (appointmentId != null) {
      if (bills.existsByAppointmentId(appointmentId))
        throw new ConflictException("Bill already exists for appointment");
      Appointment a = req(appointments.findByIdWithDetails(appointmentId), "Appointment");
      if (!a.patient.id.equals(patientId))
        throw new ConflictException("Patient does not match appointment");
      if (a.status == AppointmentStatus.CANCELLED)
        throw new ConflictException("Cannot bill a cancelled appointment");
      b.appointment = a;
      b.totalAmount = a.doctor.consultationFee.add(charges);
    } else {
      if (bills.existsByAdmissionId(admissionId))
        throw new ConflictException("Bill already exists for admission");
      Admission a = req(admissions.findByIdWithDetails(admissionId), "Admission");
      if (!a.patient.id.equals(patientId))
        throw new ConflictException("Patient does not match admission");
      if (a.status != AdmissionStatus.DISCHARGED)
        throw new ConflictException("Discharge admission before billing");
      long days = Math.max(1, Duration.between(a.admittedAt, a.dischargedAt).toDays());
      b.admission = a;
      b.totalAmount = a.room.dailyRate.multiply(BigDecimal.valueOf(days)).add(charges);
    }
    return bills.save(b);
  }

  public Bill pay(Long id) {
    Bill b = req(bills.findById(id), "Bill");
    if (b.status != BillStatus.UNPAID) throw new ConflictException("Only unpaid bills can be paid");
    b.status = BillStatus.PAID;
    b.paidAt = LocalDateTime.now();
    return bills.saveAndFlush(b);
  }
}
