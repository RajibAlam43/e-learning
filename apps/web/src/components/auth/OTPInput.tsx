'use client'

import { useRef, useCallback, type KeyboardEvent, type ClipboardEvent } from 'react'
import { useTranslations } from 'next-intl'

interface OTPInputProps {
  value: string
  onChange: (value: string) => void
}

export function OTPInput({ value, onChange }: OTPInputProps) {
  const t = useTranslations('auth.verify')
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])
  const digits = value.split('').concat(Array(6).fill('')).slice(0, 6)

  const setRef = useCallback(
    (index: number) => (el: HTMLInputElement | null) => {
      inputRefs.current[index] = el
    },
    []
  )

  const handleChange = (index: number, char: string) => {
    if (!/^\d?$/.test(char)) return
    const newDigits = [...digits]
    newDigits[index] = char
    const newValue = newDigits.join('').replace(/ +$/g, '')
    onChange(newValue)
    if (char && index < 5) {
      inputRefs.current[index + 1]?.focus()
    }
  }

  const handleKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus()
    }
  }

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault()
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    if (pasted) onChange(pasted)
  }

  return (
    <div className="flex h-auto w-full items-stretch gap-2 sm:h-[60px] sm:gap-3">
      {digits.map((digit, i) => (
        <div
          key={i}
          className={`flex-1 rounded bg-white border border-solid border-[#e5e7eb] flex flex-col items-center justify-center transition-all duration-200 focus-within:border-[#4f67d8] focus-within:ring-2 focus-within:ring-[#4f67d8]/20${
            i === 3 ? ' relative isolate' : ''
          }`}
        >
          <input
            aria-label={t('digitLabel', { number: i + 1 })}
            ref={setRef(i)}
            type="text"
            inputMode="numeric"
            maxLength={1}
            value={digit || ''}
            onChange={(e) => handleChange(i, e.target.value)}
            onKeyDown={(e) => handleKeyDown(i, e)}
            onPaste={handlePaste}
            className={`h-[56px] w-[54px] border-none bg-transparent px-2 text-center font-sans text-[22px] font-semibold leading-[125%] outline-none sm:h-[60px] sm:w-[62px] sm:px-4 sm:text-[26px] ${
              digit ? 'text-[#262626] tracking-[-1px]' : 'text-[#71717a]'
            }`}
          />
          {i === 3 && (
            <div className="absolute left-0 top-1/2 z-[1] h-[25px] w-px -translate-y-1/2 border-r border-solid border-[#4f67d8] sm:h-[29px]" />
          )}
        </div>
      ))}
    </div>
  )
}
