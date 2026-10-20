import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export const NAVIGATION = [
  { label: 'Overview', to: '/dashboard', icon: 'bi-grid-1x2' },
  { label: 'Patients', to: '/patients', icon: 'bi-person-vcard' },
  { label: 'Doctors', to: '/doctors', icon: 'bi-heart-pulse' },
  { label: 'Departments', to: '/departments', icon: 'bi-hospital' },
  { label: 'Rooms', to: '/rooms', icon: 'bi-door-closed' },
  { label: 'Appointments', to: '/appointments', icon: 'bi-calendar2-check' },
  { label: 'Admissions', to: '/admissions', icon: 'bi-clipboard2-pulse' },
  { label: 'Prescriptions', to: '/prescriptions', icon: 'bi-prescription2' },
  { label: 'Medicines', to: '/medicines', icon: 'bi-capsule' },
  { label: 'Billing', to: '/billing', icon: 'bi-receipt' },
]

export default function AppLayout() {
  const { profile, avatarUrl, signOut } = useAuth()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const navigation = profile?.role === 'ADMIN'
    ? [...NAVIGATION, { label: 'User accounts', to: '/user-accounts', icon: 'bi-person-plus' }]
    : NAVIGATION

  const currentTitle =
    navigation.find((item) => item.to === location.pathname)?.label ||
    (location.pathname === '/profile' ? 'My profile' : 'Overview')

  // Close the mobile drawer on navigation and on Escape.
  useEffect(() => {
    setSidebarOpen(false)
  }, [location.pathname])
  useEffect(() => {
    const onKey = (event) => event.key === 'Escape' && setSidebarOpen(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  const logout = () => signOut()

  return (
    <div className="app-shell">
      {sidebarOpen && (
        <button className="sidebar-scrim" aria-label="Close navigation" onClick={() => setSidebarOpen(false)} />
      )}
      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
        <NavLink to="/dashboard" className="brand">
          <span className="brand-mark"><i className="bi bi-heart-pulse-fill" /></span>
          <span><strong>CarePoint</strong><small>Hospital workspace</small></span>
        </NavLink>
        <div className="nav-label">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          {navigation.map((item) => (
            <NavLink key={item.to} to={item.to} className="side-link">
              <i className={`bi ${item.icon}`} /><span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <NavLink to="/profile" className="side-link"><i className="bi bi-person-circle" /><span>Profile</span></NavLink>
          <button className="side-link logout-link" onClick={logout}><i className="bi bi-box-arrow-left" /><span>Sign out</span></button>
          <div className="sidebar-footnote"><i className="bi bi-shield-check" /> Access is enforced by your server account</div>
        </div>
      </aside>

      <main className="main-shell">
        <header className="topbar">
          <button className="icon-button menu-toggle" aria-label="Toggle navigation" onClick={() => setSidebarOpen((open) => !open)}>
            <i className="bi bi-list" />
          </button>
          <div className="topbar-heading">
            <span className="eyebrow">HOSPITAL MANAGEMENT</span>
            <h1>{currentTitle}</h1>
          </div>
          <div className="topbar-user">
            <span className="user-avatar">
              {avatarUrl ? <img src={avatarUrl} alt="" /> : <i className="bi bi-person-fill" />}
            </span>
            <span className="user-meta">
              <strong>{profile?.username || 'Signed in'}</strong>
              <small>{profile?.role || 'Authenticated user'}</small>
            </span>
            <button className="icon-button topbar-logout" title="Sign out" aria-label="Sign out" onClick={logout}>
              <i className="bi bi-box-arrow-right" />
            </button>
          </div>
        </header>
        <section className="page-content">
          <Outlet />
        </section>
      </main>
    </div>
  )
}
