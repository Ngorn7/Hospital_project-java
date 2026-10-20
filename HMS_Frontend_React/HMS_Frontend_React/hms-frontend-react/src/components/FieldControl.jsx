/** Renders one form control for a resource field definition. */
export default function FieldControl({ id, field, value, onChange, options = [], disabled = false, required = false }) {
  const common = { id, value: value ?? '', onChange, disabled, required }
  if (field.type === 'select') {
    return (
      <select className="form-select" {...common}>
        <option value="">{field.optional ? 'None' : `Select ${field.label.toLowerCase()}`}</option>
        {options.map((option) => (
          <option key={String(option.value)} value={option.value}>{option.label}</option>
        ))}
      </select>
    )
  }
  if (field.type === 'textarea') {
    return <textarea className="form-control" rows={3} {...common} />
  }
  return (
    <input
      className="form-control"
      type={field.type || 'text'}
      min={field.min}
      step={field.step}
      {...common}
    />
  )
}
