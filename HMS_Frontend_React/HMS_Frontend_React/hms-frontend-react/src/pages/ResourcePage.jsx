import { useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../api/client'
import {
  dischargeAdmission, fetchPatient, payBill, setAppointmentStatus,
} from '../api/resources'
import { APPOINTMENT_STATUS_OPTIONS, REFERENCE_LOADERS, RESOURCES } from '../config/resources'
import FieldControl from '../components/FieldControl'
import { EmptyState, PageHeading, Spinner, StatusPill, confirmDialog, showError, toast } from '../components/ui'
import { formatCurrency, formatDate, formatDateTime, getNestedValue, pageContent, pageCount, toApiValue } from '../utils/format'

const emptyForm = (config) =>
  Object.fromEntries(config.fields.map((field) => [field.key, field.default ?? '']))

/** Converts an API record into form values, mapping nested objects (e.g. patient) to their ids. */
function formFromRecord(config, record) {
  const values = emptyForm(config)
  for (const field of config.fields) {
    const nested = field.key.endsWith('Id') ? field.key.slice(0, -2) : null
    if (nested && record[nested]) values[field.key] = record[nested].id
    else if (field.type === 'date') values[field.key] = String(record[field.key] ?? '').slice(0, 10)
    else if (field.type === 'datetime-local') values[field.key] = String(record[field.key] ?? '').slice(0, 16)
    else values[field.key] = record[field.key] ?? ''
  }
  return values
}

function displayValue(record, column) {
  const value = getNestedValue(record, column.key)
  if (value == null || value === '') return '—'
  if (column.currency) return formatCurrency(value)
  if (['appointmentAt', 'admittedAt', 'dischargedAt', 'paidAt'].includes(column.key)) return formatDateTime(value)
  if (['dateOfBirth', 'issuedAt'].includes(column.key)) return formatDate(value)
  return value
}

export default function ResourcePage({ resourceKey }) {
  const config = RESOURCES[resourceKey]
  const { profile } = useAuth()
  const isAdmin = profile?.role === 'ADMIN'

  const [records, setRecords] = useState([])
  const [page, setPage] = useState(0)
  const [pages, setPages] = useState(1)
  const [query, setQuery] = useState('')
  const [serverQuery, setServerQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [dateFilter, setDateFilter] = useState('')
  const [lowStockOnly, setLowStockOnly] = useState(false)
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState('')

  const [showForm, setShowForm] = useState(false)
  const [editingRecord, setEditingRecord] = useState(null)
  const [form, setForm] = useState(() => emptyForm(config))
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)
  const [refs, setRefs] = useState({})
  const [refError, setRefError] = useState('')

  const [patientDetail, setPatientDetail] = useState(null)

  const loadRecords = useCallback(async () => {
    setLoading(true)
    setListError('')
    try {
      const result = await config.load(page, { query: serverQuery, lowStockOnly })
      setRecords(pageContent(result))
      setPages(pageCount(result))
    } catch (error) {
      setListError(errorMessage(error))
    } finally {
      setLoading(false)
    }
  }, [config, page, serverQuery, lowStockOnly])

  useEffect(() => {
    loadRecords()
  }, [loadRecords])

  // Server-side search (patients) is debounced so each keystroke does not hit the API.
  useEffect(() => {
    if (!config.serverSearch) return undefined
    const timer = setTimeout(() => {
      setServerQuery(query.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [query, config.serverSearch])

  const visibleRecords = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return records.filter((record) => {
      if (!config.serverSearch && needle) {
        const matches = config.columns.some((column) =>
          String(getNestedValue(record, column.key) ?? '').toLowerCase().includes(needle))
        if (!matches) return false
      }
      if (resourceKey === 'appointments') {
        if (statusFilter && record.status !== statusFilter) return false
        if (dateFilter && !String(record.appointmentAt || '').startsWith(dateFilter)) return false
      }
      return true
    })
  }, [records, query, statusFilter, dateFilter, config, resourceKey])

  const loadReferences = useCallback(async () => {
    const needed = [...new Set(config.fields.map((field) => field.ref).filter(Boolean))]
    const results = await Promise.allSettled(needed.map((key) => REFERENCE_LOADERS[key]()))
    const next = {}
    const failures = []
    results.forEach((result, index) => {
      if (result.status === 'fulfilled') next[needed[index]] = pageContent(result.value)
      else failures.push(`${needed[index]}: ${errorMessage(result.reason)}`)
    })
    setRefs(next)
    setRefError(failures.join(' '))
  }, [config]) // eslint-disable-line react-hooks/exhaustive-deps

  const openCreate = () => {
    setEditingRecord(null)
    setForm(emptyForm(config))
    setFormError('')
    setShowForm(true)
    loadReferences()
  }

  const openEdit = (record) => {
    setEditingRecord(record)
    setForm(formFromRecord(config, record))
    setFormError('')
    setShowForm(true)
    loadReferences()
  }

  const closeForm = () => {
    setShowForm(false)
    setEditingRecord(null)
    setFormError('')
  }

  const visibleFields = config.fields.filter((field) => !field.showWhen || field.showWhen(form))

  const optionsFor = (field) => {
    if (field.options) {
      const list = typeof field.options === 'function' ? field.options(editingRecord) : field.options
      return list.map((option) => (typeof option === 'string' ? { value: option, label: option } : option))
    }
    let items = refs[field.ref] || []
    if (field.roomFilter) {
      // Only free rooms, plus the room the admission already occupies while editing it.
      items = items.filter((room) => room.status === 'AVAILABLE' || room.id === editingRecord?.room?.id)
    }
    if (field.filter) items = items.filter(field.filter)
    return items.map((item) => ({
      value: item.id,
      label: typeof field.optionLabel === 'function' ? field.optionLabel(item) : item[field.optionLabel] ?? item.id,
    }))
  }

  const buildPayload = () => {
    const payload = {}
    for (const field of config.fields) {
      if (field.virtual || (field.showWhen && !field.showWhen(form))) continue
      const raw = form[field.key]
      if (raw === '' || raw == null) payload[field.key] = null
      else if (field.type === 'number' || field.ref) payload[field.key] = Number(raw)
      else payload[field.key] = toApiValue(raw, field.type)
    }
    return payload
  }

  const submitForm = async (event) => {
    event.preventDefault()
    setFormError('')
    const missing = visibleFields.find(
      (field) => (field.required || field.requiredWhen?.(form)) && String(form[field.key] ?? '').trim() === '',
    )
    if (missing) {
      setFormError(`${missing.label} is required.`)
      return
    }
    setSaving(true)
    try {
      if (editingRecord) await config.update(editingRecord.id, buildPayload())
      else await config.create(buildPayload())
      closeForm()
      await loadRecords()
      toast(editingRecord ? 'Updated successfully' : 'Saved successfully')
    } catch (error) {
      setFormError(errorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  const removeRecord = async (record) => {
    const confirmed = await confirmDialog({
      title: 'Delete this record?',
      text: 'This cannot be undone.',
      confirmText: 'Delete',
      danger: true,
    })
    if (!confirmed) return
    try {
      await config.remove(record.id)
      await loadRecords()
      toast('Deleted successfully')
    } catch (error) {
      showError('Could not delete', errorMessage(error))
    }
  }

  const runAction = async (confirm, action, successMessage, failureTitle) => {
    if (!(await confirm())) return
    try {
      await action()
      await loadRecords()
      toast(successMessage)
    } catch (error) {
      showError(failureTitle, errorMessage(error))
    }
  }

  const changeStatus = (record, status) =>
    runAction(
      () => confirmDialog({
        title: `${status === 'CANCELLED' ? 'Cancel' : 'Update'} appointment?`,
        text: `Change this appointment to ${status.toLowerCase()}?`,
        confirmText: 'Continue',
        danger: status === 'CANCELLED',
        icon: status === 'CANCELLED' ? 'warning' : 'question',
      }),
      () => setAppointmentStatus(record.id, status),
      'Appointment updated',
      'Could not update appointment',
    )

  const discharge = (record) =>
    runAction(
      () => confirmDialog({
        title: 'Discharge this patient?',
        text: 'The admission will be marked discharged and the room released.',
        confirmText: 'Discharge',
        danger: true,
      }),
      () => dischargeAdmission(record.id),
      'Patient discharged',
      'Could not discharge patient',
    )

  const pay = (record) =>
    runAction(
      () => confirmDialog({ title: 'Record payment?', text: `Mark bill #${record.id} as paid?`, confirmText: 'Mark as paid', icon: 'question' }),
      () => payBill(record.id),
      'Payment recorded',
      'Payment was not recorded',
    )

  const openPatient = async (record) => {
    try {
      setPatientDetail(await fetchPatient(record.id))
    } catch (error) {
      setListError(errorMessage(error))
    }
  }

  const transitionsFor = (record) => {
    if (record.status === 'SCHEDULED') return ['CONFIRMED', 'CANCELLED']
    if (record.status === 'CONFIRMED') return ['COMPLETED', 'CANCELLED']
    return []
  }
  const transitionLabel = { CONFIRMED: 'Confirm', COMPLETED: 'Complete', CANCELLED: 'Cancel' }

  const addLabel = config.addLabel
  const headingActions = (
    <>
      {resourceKey === 'medicines' && (
        <button className="btn btn-light border" onClick={() => setLowStockOnly((value) => !value)}>
          <i className="bi bi-funnel me-2" />{lowStockOnly ? 'Show all stock' : 'Low stock only'}
        </button>
      )}
      {config.create && (
        <button className="btn btn-primary" onClick={() => (showForm ? closeForm() : openCreate())}>
          <i className={`bi me-2 ${showForm ? 'bi-x-lg' : 'bi-plus-lg'}`} />
          {showForm ? 'Close form' : addLabel}
        </button>
      )}
    </>
  )

  const searchPlaceholder = config.serverSearch ? config.searchPlaceholder : 'Filter this page…'

  return (
    <>
      <PageHeading title={config.title} subtitle={config.subtitle} actions={headingActions} />

      {showForm && (
        <section className="panel form-panel">
          <div className="panel-heading">
            <div>
              <h3>{editingRecord ? 'Edit record' : addLabel}</h3>
              <p>Enter the information and save to the hospital system.</p>
            </div>
          </div>
          {refError && <div className="alert alert-warning py-2">{refError} Reference data may be incomplete; check permissions or try again.</div>}
          {formError && <div className="alert alert-danger py-2" role="alert">{formError}</div>}
          <form onSubmit={submitForm} noValidate>
            <div className="form-grid">
              {visibleFields.map((field) => {
                const id = `field-${field.key}`
                const required = Boolean(field.required || field.requiredWhen?.(form))
                return (
                  <div key={field.key} className={`form-field ${field.type === 'textarea' ? 'field-wide' : ''}`}>
                    <label className="form-label" htmlFor={id}>
                      {field.label}{required && <span className="required-mark"> *</span>}
                    </label>
                    <FieldControl
                      id={id}
                      field={field}
                      value={form[field.key]}
                      options={field.type === 'select' ? optionsFor(field) : []}
                      disabled={Boolean(field.disabledWhen?.(editingRecord))}
                      required={required}
                      onChange={(event) => setForm((current) => ({ ...current, [field.key]: event.target.value }))}
                    />
                  </div>
                )
              })}
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-light border" onClick={closeForm}>Cancel</button>
              <button className="btn btn-primary" type="submit" disabled={saving}>
                {saving && <span className="spinner-border spinner-border-sm me-2" />}
                {saving ? 'Saving…' : 'Save record'}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="panel data-panel">
        <div className="table-toolbar">
          <div>
            <h3>{config.title} directory</h3>
            <p>{visibleRecords.length} record{visibleRecords.length === 1 ? '' : 's'} on this page</p>
          </div>
          <div className="table-filters">
            <div className="search-box">
              <i className="bi bi-search" />
              <input
                className="form-control"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={searchPlaceholder}
                aria-label="Search records"
              />
            </div>
            {resourceKey === 'appointments' && (
              <>
                <select className="form-select filter-select" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} aria-label="Filter by appointment status">
                  <option value="">All statuses</option>
                  {APPOINTMENT_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{status}</option>)}
                </select>
                <input className="form-control filter-date" type="date" value={dateFilter} onChange={(e) => setDateFilter(e.target.value)} aria-label="Filter by appointment date" />
              </>
            )}
          </div>
        </div>

        {listError && (
          <div className="alert alert-warning mx-3">
            {listError} <button className="btn btn-sm btn-link" onClick={loadRecords}>Retry</button>
          </div>
        )}

        {loading ? (
          <Spinner label={`Loading ${config.title.toLowerCase()}…`} />
        ) : visibleRecords.length > 0 ? (
          <div className="table-responsive">
            <table className="table resource-table align-middle">
              <thead>
                <tr>
                  {config.columns.map((column) => <th key={column.key}>{column.label}</th>)}
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {visibleRecords.map((record) => (
                  <tr key={record.id}>
                    {config.columns.map((column) => (
                      <td key={column.key}>
                        {column.pill ? (
                          <StatusPill status={record.status} />
                        ) : (
                          <span className={column.primary ? 'primary-cell' : undefined}>{displayValue(record, column)}</span>
                        )}
                      </td>
                    ))}
                    <td className="text-end action-cell">
                      {config.viewable && (
                        <button className="btn btn-sm btn-light border me-1" title="View patient" aria-label="View patient" onClick={() => openPatient(record)}>
                          <i className="bi bi-eye" />
                        </button>
                      )}
                      {resourceKey === 'appointments' && transitionsFor(record).map((status) => (
                        <button key={status} className="btn btn-sm btn-light border ms-1" title={`Set ${status.toLowerCase()}`} onClick={() => changeStatus(record, status)}>
                          {transitionLabel[status]}
                        </button>
                      ))}
                      {resourceKey === 'admissions' && record.status === 'ADMITTED' && (
                        <button className="btn btn-sm btn-outline-danger" onClick={() => discharge(record)}>Discharge</button>
                      )}
                      {resourceKey === 'billing' && record.status === 'UNPAID' && (
                        <button className="btn btn-sm btn-outline-primary ms-1" onClick={() => pay(record)}>Record payment</button>
                      )}
                      {config.update && (
                        <button className="btn btn-sm btn-light border ms-1" title="Edit" aria-label="Edit" onClick={() => openEdit(record)}>
                          <i className="bi bi-pencil" />
                        </button>
                      )}
                      {config.remove && isAdmin && (
                        <button className="btn btn-sm btn-outline-danger ms-2" title="Delete (administrators only)" aria-label="Delete" onClick={() => removeRecord(record)}>
                          <i className="bi bi-trash3" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          !listError && (
            <EmptyState
              title={query ? 'No matching records' : `No ${config.title.toLowerCase()} yet`}
              text={query ? 'Try a different search.' : 'Records added through this workspace will appear here.'}
              action={!query && config.create && (
                <button className="btn btn-sm btn-outline-primary mt-2" onClick={openCreate}>{addLabel}</button>
              )}
            />
          )
        )}

        {pages > 1 && (
          <div className="pagination-row">
            <span>Page {page + 1} of {pages}</span>
            <div>
              <button className="btn btn-sm btn-light border me-2" disabled={page === 0 || loading} onClick={() => setPage((p) => p - 1)}>
                <i className="bi bi-chevron-left" /> Previous
              </button>
              <button className="btn btn-sm btn-light border" disabled={page + 1 >= pages || loading} onClick={() => setPage((p) => p + 1)}>
                Next <i className="bi bi-chevron-right" />
              </button>
            </div>
          </div>
        )}
      </section>

      {patientDetail && (
        <div className="modal-backdrop-custom" onClick={(event) => event.target === event.currentTarget && setPatientDetail(null)}>
          <section className="detail-modal panel" role="dialog" aria-label="Patient record">
            <div className="panel-heading">
              <div>
                <span className="eyebrow">PATIENT RECORD</span>
                <h3>{patientDetail.fullName}</h3>
              </div>
              <button className="icon-button" aria-label="Close patient details" onClick={() => setPatientDetail(null)}>
                <i className="bi bi-x-lg" />
              </button>
            </div>
            <div className="detail-grid">
              <span><small>Patient ID</small><strong>{patientDetail.patientCode || `#${patientDetail.id}`}</strong></span>
              <span><small>Date of birth</small><strong>{formatDate(patientDetail.dateOfBirth)}</strong></span>
              <span><small>Gender</small><strong>{patientDetail.gender || '—'}</strong></span>
              <span><small>Phone</small><strong>{patientDetail.phone || '—'}</strong></span>
              <span><small>Email</small><strong>{patientDetail.email || '—'}</strong></span>
              <span><small>Address</small><strong>{patientDetail.address || '—'}</strong></span>
            </div>
            <button className="btn btn-light border mt-4" onClick={() => setPatientDetail(null)}>Close</button>
          </section>
        </div>
      )}
    </>
  )
}
