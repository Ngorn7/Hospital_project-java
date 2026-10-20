// Runs the real React app against the running Spring Boot backend.
// Enable with:  HMS_LIVE=1 npx vitest run src/live.integration.test.jsx
// Requires the backend on http://localhost:8080, an admin account (admin / ChangeMe!12345)
// and a receptionist account (rec1 / password1).
import { describe, expect, it, vi } from 'vitest'
import Swal from 'sweetalert2'

// SweetAlert2 does not render reliably in jsdom. Dialogs are auto-confirmed and recorded, so the
// tests still exercise the app's confirm-then-act logic and can assert on the success message.
vi.mock('sweetalert2', () => {
  const fire = vi.fn(async (options = {}) => ({ isConfirmed: Boolean(options.showCancelButton) }))
  return { default: { fire } }
})
import { fireEvent, render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './auth/AuthContext'

const suite = process.env.HMS_LIVE === '1' ? describe : describe.skip

// jsdom has no canvas; the chart is decorative for these tests.
HTMLCanvasElement.prototype.getContext = () => null

function mountAt(path) {
  window.history.pushState({}, '', path)
  return render(
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>,
  )
}

function signIn(user, password) {
  fireEvent.change(screen.getByLabelText('Username or email'), { target: { value: user } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: /sign in/i }))
}

// Creates a SCHEDULED appointment through the REST API so the UI test always has one to confirm.
async function seedScheduledAppointment() {
  const login = await (await fetch(`${window.location.origin}/api/v1/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ usernameOrEmail: 'admin', password: 'ChangeMe!12345' }) })).json()
  const auth = { Authorization: `Bearer ${login.accessToken}`, 'Content-Type': 'application/json' }
  const patients = await (await fetch(`${window.location.origin}/api/v1/patients?size=1`, { headers: auth })).json()
  const doctors = await (await fetch(`${window.location.origin}/api/v1/doctors?size=1`, { headers: auth })).json()
  const slot = new Date(Date.now() + 365 * 86400000)
  slot.setSeconds(0, 0)
  await fetch(`${window.location.origin}/api/v1/appointments`, {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({ patientId: patients.content[0].id, doctorId: doctors.content[0].id, appointmentAt: slot.toISOString().slice(0, 19), reason: 'UI test' }),
  })
}

suite('live backend', () => {
  it('admin signs in, sees the dashboard, and registers a patient', async () => {
    mountAt('/login')
    signIn('admin', 'ChangeMe!12345')
    expect(await screen.findByText(/Welcome back, admin/, {}, { timeout: 8000 })).toBeInTheDocument()
    expect(await screen.findByText('Total patients', {}, { timeout: 8000 })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('link', { name: 'Patients' }))
    await screen.findByRole('heading', { name: 'Patients', level: 2 })
    const name = `Integration ${Date.now()}`
    fireEvent.click(await screen.findByRole('button', { name: /Register patient/ }))
    fireEvent.change(document.getElementById('field-fullName'), { target: { value: name } })
    fireEvent.change(document.getElementById('field-dateOfBirth'), { target: { value: '1985-05-05' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save record' }))

    expect(await screen.findByText(name, {}, { timeout: 8000 })).toBeInTheDocument()
    // Administrators see delete controls.
    expect((await screen.findAllByRole('button', { name: 'Delete' })).length).toBeGreaterThan(0)
  }, 30000)

  it('admin confirms a scheduled appointment through the status dialog', async () => {
    mountAt('/login')
    signIn('admin', 'ChangeMe!12345')
    await screen.findByText(/Welcome back, admin/, {}, { timeout: 8000 })

    await seedScheduledAppointment()
    fireEvent.click(screen.getByRole('link', { name: 'Appointments' }))
    await screen.findByRole('heading', { name: 'Appointments', level: 2 })
    fireEvent.change(await screen.findByLabelText('Filter by appointment status'), { target: { value: 'SCHEDULED' } })
    const confirmButtons = await screen.findAllByRole('button', { name: 'Confirm' }, { timeout: 8000 })
    fireEvent.click(confirmButtons[0])
    await vi.waitFor(() =>
      expect(Swal.fire).toHaveBeenCalledWith(expect.objectContaining({ title: 'Appointment updated' })),
    { timeout: 8000 })
  }, 30000)

  it('receptionist cannot see delete controls and is blocked from admin pages', async () => {
    mountAt('/login')
    signIn('rec1', 'password1')
    expect(await screen.findByText(/Welcome back, rec1/, {}, { timeout: 8000 })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('link', { name: 'Patients' }))
    await screen.findByRole('heading', { name: 'Patients', level: 2 })
    expect((await screen.findAllByRole('button', { name: 'Edit' }, { timeout: 8000 })).length).toBeGreaterThan(0)
    expect(screen.queryAllByRole('button', { name: 'Delete' })).toHaveLength(0)
  }, 30000)

  it('receptionist opening the admin-only page sees the permission message', async () => {
    mountAt('/user-accounts')
    signIn('rec1', 'password1')
    expect(await screen.findByText('Permission required', {}, { timeout: 8000 })).toBeInTheDocument()
  }, 30000)
})
