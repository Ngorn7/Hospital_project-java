import { describe, expect, it } from 'vitest'
import { formatCurrency, formatDate, getNestedValue, pageContent, pageCount, toApiValue } from './format'

describe('format helpers', () => {
  it('formats numeric currency and rejects invalid input', () => {
    expect(formatCurrency('12.5')).toMatch(/12\.50/)
    expect(formatCurrency('not-a-number')).toBe('—')
    expect(formatCurrency(null)).toBe('—')
  })

  it('handles missing and invalid dates', () => {
    expect(formatDate(null)).toBe('—')
    expect(formatDate('not-a-date')).toBe('—')
  })

  it('normalizes array and Spring Page responses', () => {
    expect(pageContent([{ id: 1 }])).toHaveLength(1)
    expect(pageContent({ content: [{ id: 2 }] })).toHaveLength(1)
    expect(pageCount({ totalPages: 4 })).toBe(4)
    expect(pageCount([])).toBe(1)
  })

  it('reads nested fields safely', () => {
    expect(getNestedValue({ patient: { fullName: 'A' } }, 'patient.fullName')).toBe('A')
    expect(getNestedValue({}, 'patient.fullName')).toBeUndefined()
  })

  it('converts blank optional fields to null and numeric strings to numbers', () => {
    expect(toApiValue('', 'text')).toBeNull()
    expect(toApiValue('12.5', 'number')).toBe(12.5)
    expect(toApiValue('Ward', 'text')).toBe('Ward')
  })
})
