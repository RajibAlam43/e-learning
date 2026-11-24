'use client'

import type { ChangeEvent, ReactNode } from 'react'

interface FormInputProps {
  label: string
  name: string
  type?: string
  placeholder: string
  icon: ReactNode
  value: string
  onChange: (e: ChangeEvent<HTMLInputElement>) => void
  error?: string | boolean
  filled?: boolean
}

export function FormInput({
  label,
  name,
  type = 'text',
  placeholder,
  icon,
  value,
  onChange,
  error,
  filled = false,
}: FormInputProps) {
  const hasError = !!error

  return (
    <div className="flex self-stretch flex-col items-start gap-1 text-left">
      <div className="self-stretch flex items-center shrink-0">
        <div className="flex-1 text-sm font-semibold leading-5 text-[#1b1c1a]">
          {label}
        </div>
      </div>
      <div
        className={`flex h-11 self-stretch shrink-0 items-center gap-2 rounded-sm border bg-white px-3 py-2 transition-all duration-200 ${
          hasError
            ? 'border-[#fca5a5] border-solid'
            : filled
              ? 'border-[#1c3873] border-solid'
              : 'border-solid border-[#e5e7eb] focus-within:border-[#1c3873]'
        }`}
      >
        <span className="flex w-[22px] shrink-0 items-center text-[#667085]">
          {icon}
        </span>
        <input
          id={name}
          name={name}
          type={type}
          placeholder={placeholder}
          value={value}
          onChange={onChange}
          className="flex-1 border-none bg-transparent font-sans text-sm leading-[150%] text-[#5e5f5c] outline-none"
        />
      </div>
      {typeof error === 'string' && (
        <p className="self-stretch text-xs text-red-500">{error}</p>
      )}
    </div>
  )
}
