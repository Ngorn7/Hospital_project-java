import {
  createAdmission, createAppointment, createBill, createDepartment, createDoctor, createMedicine,
  createPatient, createRoom, deleteAdmission, deleteAppointment, deleteBill, deleteDepartment,
  deleteDoctor, deleteMedicine, deletePatient, deleteRoom, fetchAdmissions, fetchAppointments,
  fetchBills, fetchDepartments, fetchDoctors, fetchLowStockMedicines, fetchMedicines, fetchPatients,
  fetchRooms, updateAdmission, updateAppointment, updateDepartment, updateDoctor, updateMedicine,
  updatePatient, updateRoom,
} from '../api/resources'

// Reference data used by select fields. Each loader returns a list (or a Spring Page).
export const REFERENCE_LOADERS = {
  patients: () => fetchPatients({ page: 0, size: 100, sort: 'fullName,asc' }),
  doctors: () => fetchDoctors({ page: 0, size: 100, sort: 'fullName,asc' }),
  departments: () => fetchDepartments(),
  rooms: () => fetchRooms(),
  appointments: () => fetchAppointments({ page: 0, size: 100, sort: 'appointmentAt,desc' }),
  admissions: () => fetchAdmissions({ page: 0, size: 100, sort: 'admittedAt,desc' }),
  medicines: () => fetchMedicines(),
}

const STATUS_OPTIONS = ['SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED']
const GENDER_OPTIONS = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
]

/**
 * Each resource describes its list (columns), its form (fields) and its API calls.
 * Field types: text, email, number, date, datetime-local, textarea, select.
 * Select fields name a reference list (`ref`) and how to label each option.
 */
export const RESOURCES = {
  patients: {
    title: 'Patients',
    subtitle: 'Patient records and contact information.',
    addLabel: 'Register patient',
    serverSearch: true,
    searchPlaceholder: 'Search name, code or phone…',
    load: (page, { query }) =>
      fetchPatients({ page, size: 10, sort: 'createdAt,desc', ...(query ? { q: query } : {}) }),
    create: createPatient,
    update: updatePatient,
    remove: deletePatient,
    viewable: true,
    columns: [
      { key: 'patientCode', label: 'Patient ID' },
      { key: 'fullName', label: 'Patient', primary: true },
      { key: 'gender', label: 'Gender' },
      { key: 'dateOfBirth', label: 'Date of birth' },
      { key: 'phone', label: 'Phone' },
      { key: 'email', label: 'Email' },
    ],
    fields: [
      { key: 'fullName', label: 'Full name', required: true },
      { key: 'dateOfBirth', label: 'Date of birth', type: 'date', required: true },
      { key: 'gender', label: 'Gender', type: 'select', options: GENDER_OPTIONS, optional: true },
      { key: 'phone', label: 'Phone' },
      { key: 'email', label: 'Email', type: 'email' },
      { key: 'address', label: 'Address', type: 'textarea' },
    ],
  },

  doctors: {
    title: 'Doctors',
    subtitle: 'Medical staff, specialties and department assignments.',
    addLabel: 'Add doctor',
    load: (page) => fetchDoctors({ page, size: 10, sort: 'fullName,asc' }),
    create: createDoctor,
    update: updateDoctor,
    remove: deleteDoctor,
    columns: [
      { key: 'fullName', label: 'Doctor', primary: true },
      { key: 'specialty', label: 'Specialty' },
      { key: 'department.name', label: 'Department' },
      { key: 'phone', label: 'Phone' },
      { key: 'email', label: 'Email' },
      { key: 'consultationFee', label: 'Consultation fee', currency: true },
    ],
    fields: [
      { key: 'fullName', label: 'Full name', required: true },
      { key: 'departmentId', label: 'Department', type: 'select', required: true, ref: 'departments', optionLabel: 'name' },
      { key: 'specialty', label: 'Specialty' },
      { key: 'phone', label: 'Phone' },
      { key: 'email', label: 'Email', type: 'email' },
      { key: 'consultationFee', label: 'Consultation fee', type: 'number', min: 0, step: '0.01', required: true },
    ],
  },

  departments: {
    title: 'Departments',
    subtitle: 'Hospital departments and contact details.',
    addLabel: 'Add department',
    load: () => fetchDepartments(),
    create: createDepartment,
    update: updateDepartment,
    remove: deleteDepartment,
    columns: [
      { key: 'id', label: 'ID' },
      { key: 'name', label: 'Department', primary: true },
      { key: 'location', label: 'Location' },
      { key: 'phone', label: 'Phone' },
    ],
    fields: [
      { key: 'name', label: 'Department name', required: true },
      { key: 'location', label: 'Location' },
      { key: 'phone', label: 'Phone' },
    ],
  },

  rooms: {
    title: 'Rooms',
    subtitle: 'Room availability and daily rates.',
    addLabel: 'Add room',
    load: () => fetchRooms(),
    create: createRoom,
    update: updateRoom,
    remove: deleteRoom,
    columns: [
      { key: 'roomNumber', label: 'Room', primary: true },
      { key: 'roomType', label: 'Type' },
      { key: 'department.name', label: 'Department' },
      { key: 'dailyRate', label: 'Daily rate', currency: true },
      { key: 'status', label: 'Status', pill: true },
    ],
    fields: [
      { key: 'departmentId', label: 'Department', type: 'select', required: true, ref: 'departments', optionLabel: 'name' },
      { key: 'roomNumber', label: 'Room number', required: true },
      { key: 'roomType', label: 'Room type', required: true },
      { key: 'dailyRate', label: 'Daily rate', type: 'number', min: 0, step: '0.01', required: true },
      {
        key: 'status',
        label: 'Status',
        type: 'select',
        // Occupancy follows admissions, so OCCUPIED is only offered for a room that already is.
        options: (record) => ['AVAILABLE', 'MAINTENANCE', ...(record?.status === 'OCCUPIED' ? ['OCCUPIED'] : [])],
        default: 'AVAILABLE',
        disabledWhen: (record) => record?.status === 'OCCUPIED',
      },
    ],
  },

  appointments: {
    title: 'Appointments',
    subtitle: 'Schedule and track patient visits.',
    addLabel: 'Schedule appointment',
    load: (page) => fetchAppointments({ page, size: 10, sort: 'appointmentAt,desc' }),
    create: createAppointment,
    update: updateAppointment,
    remove: deleteAppointment,
    columns: [
      { key: 'id', label: 'ID' },
      { key: 'patient.fullName', label: 'Patient', primary: true },
      { key: 'doctor.fullName', label: 'Doctor' },
      { key: 'appointmentAt', label: 'Date & time' },
      { key: 'status', label: 'Status', pill: true },
      { key: 'reason', label: 'Reason' },
    ],
    fields: [
      { key: 'patientId', label: 'Patient', type: 'select', required: true, ref: 'patients', optionLabel: 'fullName' },
      { key: 'doctorId', label: 'Doctor', type: 'select', required: true, ref: 'doctors', optionLabel: 'fullName' },
      { key: 'appointmentAt', label: 'Appointment date & time', type: 'datetime-local', required: true },
      { key: 'reason', label: 'Reason', type: 'textarea' },
    ],
  },

  admissions: {
    title: 'Admissions',
    subtitle: 'Inpatient stays, assigned rooms and discharge.',
    addLabel: 'Admit patient',
    load: (page) => fetchAdmissions({ page, size: 10, sort: 'admittedAt,desc' }),
    create: createAdmission,
    update: updateAdmission,
    remove: deleteAdmission,
    columns: [
      { key: 'id', label: 'ID' },
      { key: 'patient.fullName', label: 'Patient', primary: true },
      { key: 'doctor.fullName', label: 'Doctor' },
      { key: 'room.roomNumber', label: 'Room' },
      { key: 'admittedAt', label: 'Admitted' },
      { key: 'dischargedAt', label: 'Discharged' },
      { key: 'status', label: 'Status', pill: true },
    ],
    fields: [
      { key: 'patientId', label: 'Patient', type: 'select', required: true, ref: 'patients', optionLabel: 'fullName' },
      { key: 'doctorId', label: 'Doctor', type: 'select', required: true, ref: 'doctors', optionLabel: 'fullName' },
      // On edit, the admission's current room is kept in the list even though it is occupied.
      { key: 'roomId', label: 'Room', type: 'select', required: true, ref: 'rooms', optionLabel: 'roomNumber', roomFilter: true },
      { key: 'diagnosis', label: 'Diagnosis', type: 'textarea' },
    ],
  },

  medicines: {
    title: 'Medicines',
    subtitle: 'Medicine inventory and stock levels.',
    addLabel: 'Add medicine',
    load: (page, { lowStockOnly }) => (lowStockOnly ? fetchLowStockMedicines() : fetchMedicines()),
    create: createMedicine,
    update: updateMedicine,
    remove: deleteMedicine,
    columns: [
      { key: 'id', label: 'ID' },
      { key: 'name', label: 'Medicine', primary: true },
      { key: 'unit', label: 'Unit' },
      { key: 'unitPrice', label: 'Unit price', currency: true },
      { key: 'stockQuantity', label: 'In stock' },
    ],
    fields: [
      { key: 'name', label: 'Medicine name', required: true },
      { key: 'unit', label: 'Unit' },
      { key: 'unitPrice', label: 'Unit price', type: 'number', min: 0, step: '0.01', required: true },
      { key: 'stockQuantity', label: 'Stock quantity', type: 'number', min: 0, step: '1', required: true },
    ],
  },

  billing: {
    title: 'Billing',
    subtitle: 'Issue bills and record payments.',
    addLabel: 'Create bill',
    load: (page) => fetchBills({ page, size: 10, sort: 'issuedAt,desc' }),
    create: createBill,
    remove: deleteBill,
    columns: [
      { key: 'id', label: 'Bill', primary: false },
      { key: 'patient.fullName', label: 'Patient', primary: true },
      { key: 'issuedAt', label: 'Issued' },
      { key: 'totalAmount', label: 'Total', currency: true },
      { key: 'status', label: 'Payment status', pill: true },
      { key: 'paidAt', label: 'Paid at' },
    ],
    fields: [
      { key: 'patientId', label: 'Patient', type: 'select', required: true, ref: 'patients', optionLabel: 'fullName' },
      {
        key: 'sourceType',
        label: 'Bill for',
        type: 'select',
        options: [
          { value: 'APPOINTMENT', label: 'Appointment' },
          { value: 'ADMISSION', label: 'Admission' },
        ],
        required: true,
        default: 'APPOINTMENT',
        virtual: true,
      },
      {
        key: 'appointmentId',
        label: 'Appointment',
        type: 'select',
        ref: 'appointments',
        optionLabel: (a) => `#${a.id} · ${a.patient?.fullName ?? ''} · ${a.doctor?.fullName ?? ''}`,
        showWhen: (form) => form.sourceType === 'APPOINTMENT',
        requiredWhen: (form) => form.sourceType === 'APPOINTMENT',
        filter: (a) => a.status !== 'CANCELLED',
      },
      {
        key: 'admissionId',
        label: 'Discharged admission',
        type: 'select',
        ref: 'admissions',
        optionLabel: (a) => `#${a.id} · ${a.patient?.fullName ?? ''} · Room ${a.room?.roomNumber ?? ''}`,
        showWhen: (form) => form.sourceType === 'ADMISSION',
        requiredWhen: (form) => form.sourceType === 'ADMISSION',
        filter: (a) => a.status === 'DISCHARGED',
      },
      { key: 'additionalCharges', label: 'Additional charges', type: 'number', min: 0, step: '0.01', default: '0', optional: true },
    ],
  },
}

export const APPOINTMENT_STATUS_OPTIONS = STATUS_OPTIONS
