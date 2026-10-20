export function formatDate(value, options = { year: 'numeric', month: 'short', day: 'numeric' }) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat(undefined, options).format(date)
}

export function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit',
  }).format(date)
}

export function formatCurrency(value) {
  const amount = Number(value)
  if (value === null || value === '' || !Number.isFinite(amount)) return '—'
  return new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount)
}

/** Accepts a Spring Page ({ content }) or a plain array. */
export function pageContent(result) {
  return Array.isArray(result) ? result : Array.isArray(result?.content) ? result.content : []
}

export function pageCount(result) {
  return Number.isInteger(result?.totalPages) && result.totalPages > 0 ? result.totalPages : 1
}

export function getNestedValue(record, key) {
  return key.split('.').reduce((value, part) => value?.[part], record)
}

/** Turns "" into null for optional fields, numeric strings into numbers. */
export function toApiValue(value, type) {
  if (value === '' || value === undefined) return null
  if (type === 'number') return Number(value)
  return value
}

export function statusClass(status) {
  return `status-${String(status || '').toLowerCase()}`
}
