import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../api/client'

export default function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [usernameOrEmail, setUsernameOrEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    if (!usernameOrEmail.trim() || !password) {
      setError('Enter your username or email and password.')
      return
    }
    setPending(true)
    try {
      await signIn(usernameOrEmail.trim(), password)
      navigate(location.state?.from || '/dashboard', { replace: true })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-brand-panel">
        <Link to="/login" className="brand login-brand">
          <span className="brand-mark"><i className="bi bi-heart-pulse-fill" /></span>
          <span><strong>CarePoint</strong><small>Hospital workspace</small></span>
        </Link>
        <div className="login-message">
          <span className="login-overline">CARE, CONNECTED</span>
          <h1>Better coordination.<br /><span>More time for care.</span></h1>
          <p>A secure workspace for the people who keep your hospital moving.</p>
          <div className="login-feature"><i className="bi bi-shield-check" /> Protected by your hospital&apos;s secure sign-in</div>
        </div>
        <img className="login-illustration" src="/clinic-illustration.svg" alt="" />
        <div className="login-brand-footer">CAREPOINT HOSPITAL MANAGEMENT</div>
      </section>

      <section className="login-form-panel">
        <div className="login-form-wrap">
          <span className="login-mobile-mark"><i className="bi bi-heart-pulse-fill" /></span>
          <div className="eyebrow">WELCOME BACK</div>
          <h2>Sign in to your account</h2>
          <p className="muted-copy">Use the credentials provided by your administrator.</p>
          <form className="login-form" onSubmit={submit} noValidate>
            {error && (
              <div className="alert alert-danger login-alert" role="alert">
                <i className="bi bi-exclamation-circle me-2" />{error}
              </div>
            )}
            <label className="form-label" htmlFor="identity">Username or email</label>
            <div className="input-with-icon">
              <i className="bi bi-person" />
              <input id="identity" className="form-control" autoComplete="username" value={usernameOrEmail} onChange={(e) => setUsernameOrEmail(e.target.value)} />
            </div>

            <div className="password-label"><label className="form-label" htmlFor="password">Password</label></div>
            <div className="input-with-icon password-input">
              <i className="bi bi-lock" />
              <input id="password" className="form-control" type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
              <button type="button" className="password-toggle" aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword((v) => !v)}>
                <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`} />
              </button>
            </div>

            <button className="btn btn-primary login-submit" type="submit" disabled={pending}>
              {pending && <span className="spinner-border spinner-border-sm me-2" />}
              {pending ? 'Signing in…' : 'Sign in'}
              {!pending && <i className="bi bi-arrow-right ms-auto" />}
            </button>
          </form>
          <p className="login-help"><i className="bi bi-info-circle" /> For account access, contact your hospital administrator.</p>
        </div>
      </section>
    </main>
  )
}
