'use client'

import Image from 'next/image'
import { useState, useId, type ReactNode } from 'react'
import { Check, ChevronDown } from 'lucide-react'

interface FilterSectionProps {
  title: string
  iconSrc: string
  children: ReactNode
  defaultOpen?: boolean
}

export function FilterSection({ title, iconSrc, children, defaultOpen = true }: FilterSectionProps) {
  const [open, setOpen] = useState(defaultOpen)
  const contentId = useId()
  return (
    <section className="flex flex-col items-start gap-[16px] rounded-lg bg-white p-[16px]">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        aria-controls={contentId}
        className="flex w-full items-center justify-between gap-5 cursor-pointer"
      >
        <div className="inline-flex items-center justify-center gap-[8px]">
          <Image src={iconSrc} alt="" width={24} height={24} className="relative h-6 w-6 p-[3px]" />
          <span className="font-sans text-base font-medium leading-6 text-[#1b1c1a]">
            {title}
          </span>
        </div>
        <ChevronDown
          className={`h-6 w-6 text-[#1b1c1a] transition-transform duration-200 ${open ? '' : '-rotate-90'}`}
        />
      </button>
      {open && (
        <>
          <div className="h-px w-full border-t border-[#e8ebf1]" />
          <div id={contentId} className="flex w-full flex-col gap-[12px] font-sans text-sm text-[#5e5f5c]">
            {children}
          </div>
        </>
      )}
    </section>
  )
}

export function RadioOption({ label, checked, onChange }: { label: string; checked: boolean; onChange: () => void }) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-3">
      <input type="radio" checked={checked} onChange={onChange} className="peer sr-only" />
      <span className="inline-flex items-center justify-center" aria-hidden="true">
        <span
          className={`relative h-5 w-5 rounded-[10px] border border-solid border-[#5e5f5c] bg-white transition-all duration-200 peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-[#1c3873] ${
            checked
              ? 'after:absolute after:top-1/2 after:left-1/2 after:h-2.5 after:w-2.5 after:-translate-x-1/2 after:-translate-y-1/2 after:rounded-full after:bg-[#5e5f5c] after:content-[""]'
              : ''
          }`}
        />
      </span>
      <span className="font-sans text-sm leading-[150%] text-[#5e5f5c]">{label}</span>
    </label>
  )
}

export function CheckboxOption({ label, checked, onChange }: { label: string; checked: boolean; onChange: () => void }) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-3">
      <span className="inline-flex items-center justify-center">
        <input type="checkbox" checked={checked} onChange={onChange} className="peer sr-only" aria-label={label} />
        <span
          aria-hidden="true"
          className={`relative flex h-5 w-5 items-center justify-center rounded-[4px] border border-solid transition-all duration-200 peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-[#1c3873] ${
            checked ? 'border-[#1c3873] bg-[#1c3873]' : 'border-[#5e5f5c] bg-white'
          }`}
        >
          {checked && <Check className="h-3.5 w-3.5 text-white" strokeWidth={3} />}
        </span>
      </span>
      <span className="font-sans text-sm leading-[21px] text-[#5e5f5c]">{label}</span>
    </label>
  )
}
