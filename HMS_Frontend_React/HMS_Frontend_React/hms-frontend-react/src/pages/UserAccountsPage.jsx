import { useState } from 'react'
import { registerAccount } from '../api/authApi'
import { errorMessage } from '../api/client'
import { toast } from '../components/ui'

const emptyAccount = { username: '', email: '', password: '', role: 'DOCTOR' }

export default function UserAccountsPage() {
  const [form, setForm] = useState(emptyAccount)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')

  const update = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setPending(true)
    try {
      const status = await registerAccount({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        role: form.role,
      })
      setForm(emptyAccount)
      toast(status === 201 ? 'Login account created' : 'Account created')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">ADMINISTRATION</span>
          <h2>Create a login account</h2>
          <p className="muted-copy">Give a doctor or receptionist their own credentials to sign in.</p>
        </div>
      </div>

      <section className="panel account-panel">
        <div className="panel-heading">
          <div>
            <h3>New staff account</h3>
            <p>This creates login credentials, not a doctor&apos;s clinical profile.</p>
          </div>
          <span className="panel-icon"><i className="bi bi-person-plus" /></span>
        </div>

        <div className="alert alert-info account-note">
          <i className="bi bi-info-circle me-2" />
          To add a doctor to the doctors directory too, create their login account here and then add their separate clinical record under <strong>Doctors</strong>.
        </div>
        {error && <div className="alert alert-danger" role="alert"><i className="bi bi-exclamation-circle me-2" />{error}</div>}

        <form className="account-form" onSubmit={submit} noValidate>
          <div className="form-grid">
            <div className="form-field">
              <label className="form-label" htmlFor="account-role">Account role <span className="required-mark">*</span></label>
              <select id="account-role" className="form-select" value={form.role} onChange={update('role')} required>
                <option value="DOCTOR">Doctor</option>
                <option value="RECEPTIONIST">Receptionist</option>
              </select>
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="account-username">Username <span className="required-mark">*</span></label>
              <input id="account-username" className="form-control" autoComplete="off" maxLength={80} value={form.username} onChange={update('username')} required />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="account-email">Email <span className="required-mark">*</span></label>
              <input id="account-email" className="form-control" type="email" autoComplete="off" value={form.email} onChange={update('email')} required />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="account-password">Temporary password <span className="required-mark">*</span></label>
              <input id="account-password" className="form-control" type="password" minLength={8} autoComplete="new-password" value={form.password} onChange={update('password')} required />
              <span className="field-hint">At least 8 characters. Give this privately to the new account holder.</span>
            </div>
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" type="submit" disabled={pending}>
              {pending ? <><span className="spinner-border spinner-border-sm me-2" />Creating account…</> : <><i className="bi bi-person-check me-2" />Create account</>}
            </button>
          </div>
        </form>
      </section>

      <section className="account-access-note">
        <i className="bi bi-shield-check" />
        <span><strong>Admin-only action.</strong> The server requires an authenticated ADMIN account for this endpoint. New accounts sign in with their assigned role.</span>
      </section>
    </>
  )
}
