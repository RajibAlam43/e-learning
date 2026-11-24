'use client'

import { useTranslations } from 'next-intl'

import { useState, type ChangeEvent } from 'react'
import { Eye, EyeOff } from 'lucide-react'

interface PasswordInputProps {
  label: string
  name: string
  placeholder?: string
  value: string
  onChange: (e: ChangeEvent<HTMLInputElement>) => void
  success?: boolean
  error?: string
  disabled?: boolean
}

export function PasswordInput({
  label,
  name,
  placeholder = '••••••••',
  value,
  onChange,
  success = false,
  error,
  disabled = false,
}: PasswordInputProps) {
  const ui = useTranslations('privateUi')

  const [visible, setVisible] = useState(false)

  return (
    <div className="flex self-stretch flex-col items-start gap-1 text-left">
      <div className="self-stretch flex items-center shrink-0">
        <div className="flex-1 text-sm font-semibold leading-5 text-[#1b1c1a]">
          {label}
        </div>
      </div>
      <div
        className={`flex h-11 self-stretch shrink-0 items-center gap-2 rounded-sm border bg-white px-[11px] py-2 transition-all duration-200 ${
          error
            ? 'border-[#fca5a5] border-solid'
            : success
              ? 'border-solid border-[#22c55e]'
              : 'border-solid border-[#e5e7eb] focus-within:border-[#1c3873]'
        }`}
      >
        <input
          id={name}
          name={name}
          type={visible ? 'text' : 'password'}
          placeholder={placeholder}
          value={value}
          onChange={onChange}
          disabled={disabled}
          className="flex-1 border-none bg-transparent font-sans text-sm leading-[150%] text-[#71717a] outline-none disabled:cursor-not-allowed disabled:opacity-60"
        />
        <button
          type="button"
          onClick={() => setVisible(!visible)}
          disabled={disabled}
          className="flex h-[22px] w-[22px] shrink-0 cursor-pointer items-center border-none bg-transparent p-0 text-[#667085] disabled:cursor-not-allowed disabled:opacity-40"
          aria-label={visible ? ui('hidePassword') : ui('showPassword')}
        >
          {visible ? <EyeOff className="h-[22px] w-[22px]" /> : <Eye className="h-[22px] w-[22px]" />}
        </button>
      </div>
      {error && (
        <p className="self-stretch text-xs text-red-500">{error}</p>
      )}
    </div>
  )
}
