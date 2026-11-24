'use client'

import { useState, useEffect } from 'react'
import { Search } from 'lucide-react'

interface SearchInputProps {
  value?: string
  onChange: (value: string) => void
  placeholder?: string
  className?: string
}

export function SearchInput({ value: controlledValue, onChange, placeholder = 'Search...', className }: SearchInputProps) {
  const [input, setInput] = useState(controlledValue ?? '')
  const displayValue = controlledValue ?? input

  useEffect(() => {
    const timer = setTimeout(() => onChange(displayValue), 400)
    return () => clearTimeout(timer)
  }, [displayValue, onChange])

  return (
    <div className={`relative flex items-center ${className ?? ''}`}>
      <Search className="absolute left-3 h-4 w-4 text-[#71717a]" />
      <input
        type="text"
        value={displayValue}
        onChange={(e) => setInput(e.target.value)}
        placeholder={placeholder}
        className="h-10 w-full rounded-lg border border-[#e5e5e5] bg-white pl-10 pr-3 text-sm text-[#1b1c1a] outline-none transition-colors focus:border-[#1c3873]"
      />
    </div>
  )
}
