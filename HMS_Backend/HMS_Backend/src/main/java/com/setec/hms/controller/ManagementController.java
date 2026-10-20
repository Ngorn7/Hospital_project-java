package com.setec.hms.controller;

import com.setec.hms.entity.*;
import com.setec.hms.repository.*;
import com.setec.hms.service.*;
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
public class ManagementController {
  final HmsService s;
  final PatientRepository patients;
  final DepartmentRepository deps;
  final DoctorRepository docs;
  final RoomRepository rooms;
  final MedicineRepository meds;

  public ManagementController(
      HmsService s,
      PatientRepository p,
      DepartmentRepository d,
      DoctorRepository dr,
      RoomRepository r,
      MedicineRepository m) {
    this.s = s;
    patients = p;
    deps = d;
    docs = dr;
    rooms = r;
    meds = m;
  }

  record PatientReq(
      @NotBlank String fullName,
      @NotNull LocalDate dateOfBirth,
      PatientGender gender,
      String phone,
      @Email String email,
      String address) {
  }

  record DepartmentReq(@NotBlank String name, String location, String phone) {
  }

  record DoctorReq(
      @NotNull Long departmentId,
      @NotBlank String fullName,
      String specialty,
      String phone,
      @Email String email,
      @NotNull @DecimalMin("0") BigDecimal consultationFee) {
  }

  record RoomReq(
      @NotNull Long departmentId,
      @NotBlank String roomNumber,
      @NotBlank String roomType,
      @NotNull @DecimalMin("0") BigDecimal dailyRate,
      RoomStatus status) {
  }

  record MedicineReq(
      @NotBlank String name,
      String unit,
      @NotNull @DecimalMin("0") BigDecimal unitPrice,
      @NotNull @Min(0) Integer stockQuantity) {
  }

  record DepartmentView(Long id, String name, String location, String phone) {
  }

  record DoctorView(
      Long id,
      String fullName,
      String specialty,
      String phone,
      String email,
      BigDecimal consultationFee,
      DepartmentView department,
      LocalDateTime createdAt) {
  }

  record RoomView(
      Long id,
      String roomNumber,
      String roomType,
      BigDecimal dailyRate,
      RoomStatus status,
      DepartmentView department,
      LocalDateTime createdAt) {
  }

  private DepartmentView toDepartmentView(Department department) {
    return new DepartmentView(
        department.id, department.name, department.location, department.phone);
  }

  private DoctorView toDoctorView(Doctor doctor) {
    return new DoctorView(
        doctor.id,
        doctor.fullName,
        doctor.specialty,
        doctor.phone,
        doctor.email,
        doctor.consultationFee,
        toDepartmentView(doctor.department),
        doctor.createdAt);
  }

  private RoomView toRoomView(Room room) {
    return new RoomView(
        room.id,
        room.roomNumber,
        room.roomType,
        room.dailyRate,
        room.status,
        toDepartmentView(room.department),
        room.createdAt);
  }

  // --- PATIENTS ---
  @PostMapping("/patients")
  ResponseEntity<Patient> patient(@jakarta.validation.Valid @RequestBody PatientReq r) {
    Patient p = new Patient();
    p.fullName = r.fullName;
    p.dateOfBirth = r.dateOfBirth;
    p.gender = r.gender == null ? null : r.gender.name();
    p.phone = r.phone;
    p.email = r.email;
    p.address = r.address;
    return ResponseEntity.status(201).body(s.patient(p));
  }

  @GetMapping("/patients")
  Page<Patient> patients(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String sort) {
    return s.patientList(q, Paging.of(page, size, sort, "fullName,asc"));
  }

  @GetMapping("/patients/{id}")
  Patient patientGet(@PathVariable Long id) {
    return s.req(patients.findById(id), "Patient");
  }

  @PutMapping("/patients/{id}")
  Patient patientUpdate(@PathVariable Long id, @jakarta.validation.Valid @RequestBody PatientReq r) {
    Patient p = s.req(patients.findById(id), "Patient");
    p.fullName = r.fullName;
    p.dateOfBirth = r.dateOfBirth;
    p.gender = r.gender == null ? null : r.gender.name();
    p.phone = r.phone;
    p.email = r.email;
    p.address = r.address;
    return patients.save(p);
  }

  @DeleteMapping("/patients/{id}")
  ResponseEntity<Void> patientDelete(@PathVariable Long id) {
    patients.delete(s.req(patients.findById(id), "Patient"));
    patients.flush();
    return ResponseEntity.noContent().build();
  }

  // --- DEPARTMENTS ---
  @PostMapping("/departments")
  ResponseEntity<Department> department(@jakarta.validation.Valid @RequestBody DepartmentReq r) {
    Department d = new Department();
    d.name = r.name;
    d.location = r.location;
    d.phone = r.phone;
    return ResponseEntity.status(201).body(deps.save(d));
  }

  @GetMapping("/departments")
  List<Department> departments() {
    return deps.findAll();
  }

  @PutMapping("/departments/{id}")
  DepartmentView departmentUpdate(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody DepartmentReq r) {
    Department d = s.req(deps.findById(id), "Department");
    d.name = r.name;
    d.location = r.location;
    d.phone = r.phone;
    return toDepartmentView(deps.save(d));
  }

  @DeleteMapping("/departments/{id}")
  ResponseEntity<Void> departmentDelete(@PathVariable Long id) {
    deps.delete(s.req(deps.findById(id), "Department"));
    deps.flush();
    return ResponseEntity.noContent().build();
  }

  // --- DOCTORS ---
  @PostMapping("/doctors")
  ResponseEntity<DoctorView> doctor(@jakarta.validation.Valid @RequestBody DoctorReq r) {
    Doctor d = new Doctor();
    d.department = s.req(deps.findById(r.departmentId), "Department");
    d.fullName = r.fullName;
    d.specialty = r.specialty;
    d.phone = r.phone;
    d.email = r.email;
    d.consultationFee = r.consultationFee;
    return ResponseEntity.status(201).body(toDoctorView(docs.save(d)));
  }

  // FIXED: Splits the sort string to prevent 500 error
  @GetMapping("/doctors")
  Page<DoctorView> doctors(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String sort) {
    return docs.findAllWithDepartment(Paging.of(page, size, sort, "fullName,asc"))
        .map(this::toDoctorView);
  }

  @PutMapping("/doctors/{id}")
  ResponseEntity<DoctorView> doctorUpdate(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody DoctorReq r) {
    Doctor d = s.req(docs.findById(id), "Doctor");
    d.department = s.req(deps.findById(r.departmentId), "Department");
    d.fullName = r.fullName;
    d.specialty = r.specialty;
    d.phone = r.phone;
    d.email = r.email;
    d.consultationFee = r.consultationFee;
    return ResponseEntity.ok(toDoctorView(docs.save(d)));
  }

  @DeleteMapping("/doctors/{id}")
  ResponseEntity<Void> doctorDelete(@PathVariable Long id) {
    docs.delete(s.req(docs.findById(id), "Doctor"));
    docs.flush();
    return ResponseEntity.noContent().build();
  }

  // --- ROOMS ---
  @PostMapping("/rooms")
  ResponseEntity<RoomView> room(@jakarta.validation.Valid @RequestBody RoomReq r) {
    Room x = new Room();
    x.department = s.req(deps.findById(r.departmentId), "Department");
    x.roomNumber = r.roomNumber;
    x.roomType = r.roomType;
    x.dailyRate = r.dailyRate;
    s.applyRoomStatus(x, r.status);
    return ResponseEntity.status(201).body(toRoomView(rooms.save(x)));
  }

  @GetMapping("/rooms")
  List<RoomView> roomList(@RequestParam(required = false) RoomStatus status) {
    List<Room> result = status == null ? rooms.findAllWithDepartment() : rooms.findByStatus(status);
    return result.stream().map(this::toRoomView).toList();
  }

  @PutMapping("/rooms/{id}")
  ResponseEntity<RoomView> roomUpdate(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody RoomReq r) {
    Room x = s.req(rooms.findById(id), "Room");
    x.department = s.req(deps.findById(r.departmentId), "Department");
    x.roomNumber = r.roomNumber;
    x.roomType = r.roomType;
    x.dailyRate = r.dailyRate;
    s.applyRoomStatus(x, r.status);
    return ResponseEntity.ok(toRoomView(rooms.save(x)));
  }

  @DeleteMapping("/rooms/{id}")
  ResponseEntity<Void> roomDelete(@PathVariable Long id) {
    rooms.delete(s.req(rooms.findById(id), "Room"));
    rooms.flush();
    return ResponseEntity.noContent().build();
  }

  // --- MEDICINES ---
  @PostMapping("/medicines")
  ResponseEntity<Medicine> medicine(@jakarta.validation.Valid @RequestBody MedicineReq r) {
    Medicine m = new Medicine();
    m.name = r.name;
    m.unit = r.unit;
    m.unitPrice = r.unitPrice;
    m.stockQuantity = r.stockQuantity;
    return ResponseEntity.status(201).body(meds.save(m));
  }

  @GetMapping("/medicines")
  List<Medicine> medicines() {
    return meds.findAll();
  }

  @GetMapping("/medicines/low-stock")
  List<Medicine> low(@RequestParam(defaultValue = "10") int threshold) {
    return meds.findAll().stream().filter(m -> m.stockQuantity < threshold).toList();
  }

  @PutMapping("/medicines/{id}")
  ResponseEntity<Medicine> medicineUpdate(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody MedicineReq r) {
    Medicine m = s.req(meds.findById(id), "Medicine");
    m.name = r.name;
    m.unit = r.unit;
    m.unitPrice = r.unitPrice;
    m.stockQuantity = r.stockQuantity;
    return ResponseEntity.ok(meds.save(m));
  }

  @DeleteMapping("/medicines/{id}")
  ResponseEntity<Void> medicineDelete(@PathVariable Long id) {
    meds.delete(s.req(meds.findById(id), "Medicine"));
    meds.flush();
    return ResponseEntity.noContent().build();
  }
}