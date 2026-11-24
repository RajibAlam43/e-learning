'use client'

import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form'

interface FormFieldProps<T extends FieldValues> {
  name: Path<T>
  control: Control<T>
  label?: string
  error?: string
  children: (field: {
    value: unknown
    onChange: (...event: unknown[]) => void
    onBlur: () => void
    ref: React.RefCallback<HTMLElement>
  }) => React.ReactNode
}

export function FormField<T extends FieldValues>({
  name,
  control,
  label,
  error,
  children,
}: FormFieldProps<T>) {
  return (
    <Controller
      name={name}
      control={control}
      render={({ field }) => (
        <div>
          {label && (
            <label className='block text-sm font-medium text-[var(--color-text-headline-secondary)] mb-1.5'>
              {label}
            </label>
          )}
          {children(field)}
          {error && (
            <p className='text-xs text-destructive mt-1'>{error}</p>
          )}
        </div>
      )}
    />
  )
}
