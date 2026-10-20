import Swal from 'sweetalert2'

export function Spinner({ label = 'Loading…' }) {
  return (
    <div className="loading-state" role="status">
      <span className="spinner-border text-primary" />
      <span>{label}</span>
    </div>
  )
}

export function EmptyState({ icon = 'bi-inbox', title, text, action }) {
  return (
    <div className="empty-state">
      <span className="empty-icon"><i className={`bi ${icon}`} /></span>
      <strong>{title}</strong>
      <span>{text}</span>
      {action}
    </div>
  )
}

export function PageHeading({ eyebrow = 'CAREPOINT WORKSPACE', title, subtitle, actions }) {
  return (
    <div className="page-heading resource-heading">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h2>{title}</h2>
        {subtitle && <p className="muted-copy">{subtitle}</p>}
      </div>
      {actions && <div className="heading-actions">{actions}</div>}
    </div>
  )
}

export function StatusPill({ status }) {
  return <span className={`status-pill status-${String(status || '').toLowerCase()}`}>{status || '—'}</span>
}

export function toast(title, icon = 'success') {
  return Swal.fire({ toast: true, position: 'top-end', timer: 2600, showConfirmButton: false, icon, title })
}

export function confirmDialog({ title, text, confirmText, danger = false, icon = 'warning' }) {
  return Swal.fire({
    title,
    text,
    icon,
    showCancelButton: true,
    confirmButtonText: confirmText,
    confirmButtonColor: danger ? '#d44848' : '#3677df',
  }).then((result) => result.isConfirmed)
}

export function showError(title, text) {
  return Swal.fire({ icon: 'error', title, text })
}
