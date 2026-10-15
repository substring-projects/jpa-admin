import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './select';

interface FieldSelectProps {
  id?: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: Array<{ value: string; label: string }>;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
}

// Prefix option values so an empty choice never violates Radix's non-empty item contract.
export function FieldSelect({ id, label, value, onChange, options, placeholder = 'Select…', required, disabled }: FieldSelectProps) {
  return (
    <Select value={value === '' ? '' : `value:${value}`} onValueChange={v => onChange(v === 'empty:' ? '' : v.slice(6))} required={required} disabled={disabled}>
      <SelectTrigger id={id} aria-label={label}><SelectValue placeholder={placeholder} /></SelectTrigger>
      <SelectContent>
        {!required && <SelectItem value="empty:">{placeholder}</SelectItem>}
        {options.filter(option => option.value !== '').map(option => (
          <SelectItem key={option.value} value={`value:${option.value}`}>{option.label}</SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
