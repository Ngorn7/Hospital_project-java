import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Chart, BarController, BarElement, CategoryScale, LinearScale, Tooltip } from 'chart.js'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../api/client'
import { fetchAppointments, fetchDashboard } from '../api/resources'
import { EmptyState, Spinner } from '../components/ui'
import { formatDateTime, pageContent, statusClass } from '../utils/format'

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip)

const metricDefs = [
  { key: 'totalPatients', label: 'Total patients', icon: 'bi-people', tone: 'blue', to: '/patients' },
  { key: 'totalDoctors', label: 'Medical staff', icon: 'bi-heart-pulse', tone: 'teal', to: '/doctors' },
  { key: 'todaysAppointments', label: "Today's appointments", icon: 'bi-calendar2-check', tone: 'violet', to: '/appointments' },
  { key: 'currentlyAdmitted', label: 'Currently admitted', icon: 'bi-hospital', tone: 'orange', to: '/admissions' },
  { key: 'totalDepartments', label: 'Departments', icon: 'bi-building', tone: 'blue', to: '/departments' },
  { key: 'availableRooms', label: 'Rooms available', icon: 'bi-door-open', tone: 'teal', to: '/rooms' },
  { key: 'unpaidBills', label: 'Unpaid bills', icon: 'bi-receipt', tone: 'orange', to: '/billing' },
  { key: 'lowStockMedicines', label: 'Low-stock medicines', icon: 'bi-capsule', tone: 'violet', to: '/medicines' },
]

export default function DashboardPage() {
  const { profile } = useAuth()
  const navigate = useNavigate()
  const [summary, setSummary] = useState(null)
  const [appointments, setAppointments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [appointmentsError, setAppointmentsError] = useState('')
  const canvasRef = useRef(null)
  const chartRef = useRef(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    setAppointmentsError('')
    try {
      setSummary(await fetchDashboard())
    } catch (err) {
      setSummary(null)
      setError(errorMessage(err))
      setLoading(false)
      return
    }
    setLoading(false)
    try {
      const page = await fetchAppointments({ page: 0, size: 5, sort: 'appointmentAt,desc' })
      setAppointments(pageContent(page).slice(0, 5))
    } catch (err) {
      setAppointmentsError(errorMessage(err))
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  // Draw the bar chart once the summary is on screen, and destroy it when the page unmounts.
  useEffect(() => {
    if (!summary || !canvasRef.current) return undefined
    chartRef.current?.destroy()
    chartRef.current = new Chart(canvasRef.current, {
      type: 'bar',
      data: {
        labels: ['Patients', 'Doctors', 'Appointments today', 'Admissions', 'Departments'],
        datasets: [{
          data: [summary.totalPatients, summary.totalDoctors, summary.todaysAppointments, summary.currentlyAdmitted, summary.totalDepartments],
          backgroundColor: ['#3c7be8', '#37b9a6', '#8a79e8', '#f2a65a', '#67a0f3'],
          borderRadius: 7,
          maxBarThickness: 38,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, tooltip: { displayColors: false } },
        scales: {
          y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: '#edf1f6' }, border: { display: false } },
          x: { grid: { display: false }, border: { display: false } },
        },
      },
    })
    return () => chartRef.current?.destroy()
  }, [summary])

  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">HOSPITAL AT A GLANCE</span>
          <h2>Welcome back, {profile?.username || 'there'}</h2>
          <p className="muted-copy">Live overview from your hospital records.</p>
        </div>
        <button className="btn btn-light border" disabled={loading} onClick={load}>
          <i className="bi bi-arrow-clockwise me-2" />Refresh
        </button>
      </div>

      {error && (
        <div className="alert alert-warning">
          <i className="bi bi-exclamation-triangle me-2" />{error}{' '}
          <span className="small">Your account may not have access to dashboard summaries.</span>
        </div>
      )}

      {loading ? (
        <div className="metric-grid">
          {Array.from({ length: 8 }, (_, i) => (
            <div key={i} className="metric-card skeleton-card"><span /><span /><span /></div>
          ))}
        </div>
      ) : (
        summary && (
          <>
            <div className="metric-grid">
              {metricDefs.map((metric) => (
                <button key={metric.key} className="metric-card" onClick={() => navigate(metric.to)}>
                  <span className={`metric-icon tone-${metric.tone}`}><i className={`bi ${metric.icon}`} /></span>
                  <span className="metric-label">{metric.label}</span>
                  <strong className="metric-value">{summary[metric.key] ?? 0}</strong>
                  <span className="metric-foot">From current records <i className="bi bi-arrow-up-right" /></span>
                </button>
              ))}
            </div>

            <div className="dashboard-lower">
              <section className="panel chart-panel">
                <div className="panel-heading">
                  <div><h3>Hospital overview</h3><p>Counts reported by the dashboard API</p></div>
                  <span className="panel-icon"><i className="bi bi-bar-chart" /></span>
                </div>
                <div className="chart-wrap"><canvas ref={canvasRef} aria-label="Hospital summary chart" /></div>
              </section>

              <section className="panel appointments-panel">
                <div className="panel-heading">
                  <div><h3>Appointments</h3><p>Latest records from your schedule</p></div>
                  <Link to="/appointments" className="text-link">View all <i className="bi bi-arrow-right" /></Link>
                </div>
                {appointmentsError && <div className="alert alert-warning py-2 small">{appointmentsError}</div>}
                {appointments.length > 0 ? (
                  <div className="appointment-list">
                    {appointments.map((appointment) => {
                      const when = new Date(appointment.appointmentAt)
                      return (
                        <div key={appointment.id} className="appointment-row">
                          <span className="appointment-date">
                            <strong>{Number.isNaN(when.getTime()) ? '—' : when.getDate()}</strong>
                            <small>{Number.isNaN(when.getTime()) ? '' : when.toLocaleString(undefined, { month: 'short' })}</small>
                          </span>
                          <span className="appointment-info">
                            <strong>{appointment.patient?.fullName || `Patient #${appointment.patient?.id ?? '—'}`}</strong>
                            <small>{appointment.doctor?.fullName || `Doctor #${appointment.doctor?.id ?? '—'}`}</small>
                          </span>
                          <span className={`appointment-status ${statusClass(appointment.status)}`}>{appointment.status || '—'}</span>
                          <span className="appointment-time">{formatDateTime(appointment.appointmentAt)}</span>
                        </div>
                      )
                    })}
                  </div>
                ) : (
                  !appointmentsError && (
                    <EmptyState
                      icon="bi-calendar2-minus"
                      title="No appointments to show"
                      text="Create an appointment to see it here."
                      action={<Link to="/appointments" className="btn btn-sm btn-outline-primary mt-2">Appointments</Link>}
                    />
                  )
                )}
              </section>
            </div>
          </>
        )
      )}
      {!loading && !summary && !error && <Spinner />}
    </>
  )
}
