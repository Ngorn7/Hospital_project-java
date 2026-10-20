import { Link } from 'react-router-dom'

function StatusCard({ icon, eyebrow, title, text }) {
  return (
    <main className="standalone-message">
      <div className="standalone-card">
        <span className="standalone-icon"><i className={`bi ${icon}`} /></span>
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p className="muted-copy">{text}</p>
        <Link to="/dashboard" className="btn btn-primary">Back to overview</Link>
      </div>
    </main>
  )
}

export function ForbiddenPage() {
  return (
    <StatusCard
      icon="bi-shield-lock"
      eyebrow="ACCESS RESTRICTED"
      title="Permission required"
      text="Your hospital account does not have permission for this operation. Access is controlled by the server."
    />
  )
}

export function NotFoundPage() {
  return (
    <StatusCard
      icon="bi-search"
      eyebrow="PAGE NOT FOUND"
      title="We couldn’t find that page"
      text="The link may be outdated or the address may have a typo."
    />
  )
}
