'use client'

import { Select, SelectTrigger, SelectContent, SelectItem, SelectValue } from '@/components/ui/select'

interface FilterOption {
  label: string
  value: string
}

interface FilterBarProps {
  filters: {
    key: string
    label: string
    options: FilterOption[]
    value: string
    onChange: (value: string) => void
  }[]
  onClear?: () => void
  showClear?: boolean
}

export function FilterBar({ filters, onClear, showClear }: FilterBarProps) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      {filters.map((filter) => (
        <Select key={filter.key} value={filter.value} onValueChange={(v) => filter.onChange(v ?? 'all')}>
          <SelectTrigger className="h-9 w-auto min-w-[130px] rounded-lg border-[#e5e5e5] bg-white text-sm">
            <SelectValue placeholder={filter.label} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{filter.label}: All</SelectItem>
            {filter.options.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>{opt.label}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      ))}
      {showClear && onClear && (
        <button
          type="button"
          onClick={onClear}
          className="h-9 rounded-lg px-3 text-sm font-medium text-[#71717a] transition-colors hover:text-[#1c3873]"
        >
          Clear
        </button>
      )}
    </div>
  )
}
