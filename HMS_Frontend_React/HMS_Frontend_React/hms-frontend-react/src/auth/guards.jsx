import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

function Spinner({ label = 'Loading…' }) {
  return (
    <div className="loading-state" role="status">
      <span className="spinner-border text-primary" />
      <span>{label}</span>
    </div>
  )
}

/** Private routes: send anonymous visitors to the login page, remembering where they were going. */
export function RequireAuth({ children }) {
  const { isAuthenticated, profile, profileError, profileLoading } = useAuth()
  const location = useLocation()
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  if (!profile && profileLoading) return <Spinner label="Loading your workspace…" />
  if (!profile && profileError) {
    return (
      <div className="standalone-message">
        <div className="standalone-card">
          <h1>Could not load your account</h1>
          <p className="muted-copy">{profileError}</p>
        </div>
      </div>
    )
  }
  return children
}

/** Login page: signed-in users are sent straight to the dashboard. */
export function GuestOnly({ children }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : children
}

/** Role-restricted routes. The server enforces the same rules; this only hides pages. */
export function RequireRole({ roles, children }) {
  const { profile, profileLoading } = useAuth()
  if (!profile) return profileLoading ? <Spinner /> : <Navigate to="/forbidden" replace />
  return roles.includes(profile.role) ? children : <Navigate to="/forbidden" replace />
}
