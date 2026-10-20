import { useEffect, useState } from 'react'
import { createPrescription, fetchPrescription } from '../api/resources'
import { errorMessage } from '../api/client'
import { REFERENCE_LOADERS } from '../config/resources'
import FieldControl from '../components/FieldControl'
import { PageHeading, toast } from '../components/ui'
import { formatDateTime, pageContent } from '../utils/format'

const emptyItem = () => ({ medicineId: '', dosage: '', frequency: '', durationDays: '', quantity: '' })
const fields = [
  { key: 'patientId', label: 'Patient', type: 'select', required: true, ref: 'patients', optionLabel: 'fullName' },
  { key: 'doctorId', label: 'Doctor', type: 'select', required: true, ref: 'doctors', optionLabel: 'fullName' },
  { key: 'admissionId', label: 'Admission (optional)', type: 'select', ref: 'admissions', optionLabel: (a) => `#${a.id} · ${a.patient?.fullName ?? ''}`, optional: true },
  { key: 'notes', label: 'Notes', type: 'textarea' },
]

export default function PrescriptionsPage() {
  const [form, setForm] = useState({ patientId: '', doctorId: '', admissionId: '', notes: '' })
  const [items, setItems] = useState([emptyItem()])
  const [refs, setRefs] = useState({})
  const [refError, setRefError] = useState('')
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)
  const [lookupId, setLookupId] = useState('')
  const [lookupResult, setLookupResult] = useState(null)
  const [lookupError, setLookupError] = useState('')
  const [lookupBusy, setLookupBusy] = useState(false)

  useEffect(() => {
    const keys = ['patients', 'doctors', 'admissions', 'medicines']
    Promise.allSettled(keys.map((key) => REFERENCE_LOADERS[key]()))
      .then((results) => {
        const next = {}
        const failures = []
        results.forEach((result, index) => {
          if (result.status === 'fulfilled') next[keys[index]] = pageContent(result.value)
          else failures.push(`${keys[index]}: ${errorMessage(result.reason)}`)
        })
        setRefs(next)
        setRefError(failures.join(' '))
      })
  }, [])

  const updateItem = (index, key, value) =>
    setItems((current) => current.map((item, i) => (i === index ? { ...item, [key]: value } : item)))

  const submit = async (event) => {
    event.preventDefault()
    setFormError('')
    if (items.some((item) => !item.medicineId || !item.dosage || !item.frequency || !item.durationDays || !item.quantity)) {
      setFormError('Complete the medicine, dosage, frequency, duration and quantity for each row.')
      return
    }
    setSaving(true)
    try {
      const created = await createPrescription({
        patientId: Number(form.patientId),
        doctorId: Number(form.doctorId),
        admissionId: form.admissionId ? Number(form.admissionId) : null,
        notes: form.notes || null,
        items: items.map((item) => ({
          medicineId: Number(item.medicineId),
          dosage: item.dosage,
          frequency: item.frequency,
          durationDays: Number(item.durationDays),
          quantity: Number(item.quantity),
        })),
      })
      setForm({ patientId: '', doctorId: '', admissionId: '', notes: '' })
      setItems([emptyItem()])
      toast(`Prescription #${created.id} created`)
    } catch (error) {
      setFormError(errorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  const lookup = async (event) => {
    event.preventDefault()
    setLookupResult(null)
    setLookupError('')
    if (!lookupId || Number(lookupId) < 1) {
      setLookupError('Enter a valid prescription ID.')
      return
    }
    setLookupBusy(true)
    try {
      setLookupResult(await fetchPrescription(Number(lookupId)))
    } catch (error) {
      setLookupError(errorMessage(error))
    } finally {
      setLookupBusy(false)
    }
  }

  const optionsFor = (field) =>
    (refs[field.ref] || []).map((item) => ({
      value: item.id,
      label: typeof field.optionLabel === 'function' ? field.optionLabel(item) : item[field.optionLabel] ?? item.id,
    }))

  return (
    <>
      <PageHeading
        title="Prescriptions"
        subtitle="Create prescriptions and look up a saved prescription by its ID."
      />

      <section className="panel form-panel">
        <div className="panel-heading"><div><h3>New prescription</h3><p>Stock is deducted from the medicine inventory when saved.</p></div></div>
        {refError && <div className="alert alert-warning py-2">{refError}</div>}
        {formError && <div className="alert alert-danger py-2" role="alert">{formError}</div>}
        <form onSubmit={submit} noValidate>
          <div className="form-grid">
            {fields.map((field) => (
              <div key={field.key} className={`form-field ${field.type === 'textarea' ? 'field-wide' : ''}`}>
                <label className="form-label" htmlFor={`rx-${field.key}`}>
                  {field.label}{field.required && <span className="required-mark"> *</span>}
                </label>
                <FieldControl
                  id={`rx-${field.key}`}
                  field={field}
                  value={form[field.key]}
                  options={field.type === 'select' ? optionsFor(field) : []}
                  required={Boolean(field.required)}
                  onChange={(event) => setForm((current) => ({ ...current, [field.key]: event.target.value }))}
                />
              </div>
            ))}
          </div>

          <div className="prescription-items">
            <div className="section-divider">
              <span>MEDICATIONS</span>
              <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => setItems((c) => [...c, emptyItem()])}>
                <i className="bi bi-plus-lg me-1" />Add medicine
              </button>
            </div>
            {items.map((item, index) => (
              <div key={index} className="medicine-entry">
                <div className="medicine-entry-title">
                  <strong>Medicine {index + 1}</strong>
                  {items.length > 1 && (
                    <button type="button" className="icon-button text-danger" aria-label="Remove medicine" onClick={() => setItems((c) => c.filter((_, i) => i !== index))}>
                      <i className="bi bi-trash3" />
                    </button>
                  )}
                </div>
                <div className="form-grid">
                  <div className="form-field">
                    <label className="form-label">Medicine *</label>
                    <select className="form-select" value={item.medicineId} onChange={(e) => updateItem(index, 'medicineId', e.target.value)}>
                      <option value="">Select medicine</option>
                      {(refs.medicines || []).map((med) => (
                        <option key={med.id} value={med.id}>{med.name} · stock {med.stockQuantity}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-field">
                    <label className="form-label">Dosage *</label>
                    <input className="form-control" value={item.dosage} placeholder="e.g. 500 mg" onChange={(e) => updateItem(index, 'dosage', e.target.value)} />
                  </div>
                  <div className="form-field">
                    <label className="form-label">Frequency *</label>
                    <input className="form-control" value={item.frequency} placeholder="e.g. twice daily" onChange={(e) => updateItem(index, 'frequency', e.target.value)} />
                  </div>
                  <div className="form-field">
                    <label className="form-label">Duration (days) *</label>
                    <input className="form-control" type="number" min="1" step="1" value={item.durationDays} onChange={(e) => updateItem(index, 'durationDays', e.target.value)} />
                  </div>
                  <div className="form-field">
                    <label className="form-label">Quantity *</label>
                    <input className="form-control" type="number" min="1" step="1" value={item.quantity} onChange={(e) => updateItem(index, 'quantity', e.target.value)} />
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="form-actions">
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving && <span className="spinner-border spinner-border-sm me-2" />}
              {saving ? 'Saving…' : 'Save prescription'}
            </button>
          </div>
        </form>
      </section>

      <section className="panel lookup-panel">
        <div className="panel-heading">
          <div>
            <h3>Find a prescription</h3>
            <p>The API supports lookup by prescription ID, but does not expose a list endpoint.</p>
          </div>
        </div>
        <form className="lookup-form" onSubmit={lookup}>
          <label className="visually-hidden" htmlFor="prescription-id">Prescription ID</label>
          <input id="prescription-id" type="number" min="1" className="form-control" placeholder="Enter prescription ID" value={lookupId} onChange={(e) => setLookupId(e.target.value)} />
          <button className="btn btn-primary" type="submit" disabled={lookupBusy}>Look up</button>
        </form>
        {lookupError && <div className="alert alert-danger mt-3 mb-0">{lookupError}</div>}
        {lookupResult && (
          <div className="prescription-result">
            <div className="result-summary">
              <span><small>Prescription</small><strong>#{lookupResult.id}</strong></span>
              <span><small>Patient</small><strong>{lookupResult.patient?.fullName || `#${lookupResult.patient?.id ?? '—'}`}</strong></span>
              <span><small>Doctor</small><strong>{lookupResult.doctor?.fullName || `#${lookupResult.doctor?.id ?? '—'}`}</strong></span>
              <span><small>Prescribed</small><strong>{formatDateTime(lookupResult.prescribedAt)}</strong></span>
            </div>
            {lookupResult.notes && <p className="prescription-notes">{lookupResult.notes}</p>}
            <div className="table-responsive">
              <table className="table align-middle">
                <thead><tr><th>Medicine</th><th>Dosage</th><th>Frequency</th><th>Duration</th><th>Quantity</th></tr></thead>
                <tbody>
                  {(lookupResult.items || []).map((item) => (
                    <tr key={item.id}>
                      <td>{item.medicine?.name || `Medicine #${item.medicine?.id ?? '—'}`}</td>
                      <td>{item.dosage || '—'}</td>
                      <td>{item.frequency || '—'}</td>
                      <td>{item.durationDays ?? '—'} days</td>
                      <td>{item.quantity ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>
    </>
  )
}
