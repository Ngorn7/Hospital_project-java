import api from './client'

// Each function maps to one backend route. Paged endpoints accept { page, size, sort } and optionally q.
const get = async (url, params) => (await api.get(url, { params })).data
const post = async (url, body) => (await api.post(url, body)).data
const put = async (url, body) => (await api.put(url, body)).data
const patch = async (url, body, params) => (await api.patch(url, body, { params })).data
const del = async (url) => (await api.delete(url)).data

export const fetchDashboard = () => get('/dashboard')

export const fetchPatients = (params) => get('/patients', params)
export const fetchPatient = (id) => get(`/patients/${id}`)
export const createPatient = (body) => post('/patients', body)
export const updatePatient = (id, body) => put(`/patients/${id}`, body)
export const deletePatient = (id) => del(`/patients/${id}`)

export const fetchDoctors = (params) => get('/doctors', params)
export const createDoctor = (body) => post('/doctors', body)
export const updateDoctor = (id, body) => put(`/doctors/${id}`, body)
export const deleteDoctor = (id) => del(`/doctors/${id}`)

export const fetchDepartments = () => get('/departments')
export const createDepartment = (body) => post('/departments', body)
export const updateDepartment = (id, body) => put(`/departments/${id}`, body)
export const deleteDepartment = (id) => del(`/departments/${id}`)

export const fetchRooms = (params) => get('/rooms', params)
export const createRoom = (body) => post('/rooms', body)
export const updateRoom = (id, body) => put(`/rooms/${id}`, body)
export const deleteRoom = (id) => del(`/rooms/${id}`)

export const fetchAppointments = (params) => get('/appointments', params)
export const createAppointment = (body) => post('/appointments', body)
export const updateAppointment = (id, body) => put(`/appointments/${id}`, body)
export const deleteAppointment = (id) => del(`/appointments/${id}`)
export const setAppointmentStatus = (id, status) => patch(`/appointments/${id}/status`, null, { status })

export const fetchAdmissions = (params) => get('/admissions', params)
export const createAdmission = (body) => post('/admissions', body)
export const updateAdmission = (id, body) => put(`/admissions/${id}`, body)
export const deleteAdmission = (id) => del(`/admissions/${id}`)
export const dischargeAdmission = (id) => patch(`/admissions/${id}/discharge`)

export const fetchPrescription = (id) => get(`/prescriptions/${id}`)
export const createPrescription = (body) => post('/prescriptions', body)

export const fetchMedicines = () => get('/medicines')
export const fetchLowStockMedicines = (threshold = 10) => get('/medicines/low-stock', { threshold })
export const createMedicine = (body) => post('/medicines', body)
export const updateMedicine = (id, body) => put(`/medicines/${id}`, body)
export const deleteMedicine = (id) => del(`/medicines/${id}`)

export const fetchBills = (params) => get('/bills', params)
export const createBill = (body) => post('/bills', body)
export const payBill = (id) => patch(`/bills/${id}/pay`)
export const deleteBill = (id) => del(`/bills/${id}`)
