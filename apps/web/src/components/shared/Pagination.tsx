'use client'

import { useRouter, useSearchParams } from 'next/navigation'
import {
  ChevronsLeft,
  ChevronLeft,
  ChevronRight,
  ChevronsRight,
} from 'lucide-react'

interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange?: (page: number) => void
  useUrlParams?: boolean
}

const PAGE_BTN =
  'flex h-8 w-8 cursor-pointer items-center justify-center rounded-lg border border-[#fdddb9] bg-white p-2.5 text-[13px] font-semibold text-[#5e5f5c] transition-colors hover:bg-[#f5f5f5] box-border'

const ACTIVE_BTN =
  'flex h-8 w-8 cursor-pointer items-center justify-center rounded-lg border border-[#fdddb9] bg-[#8c7355] p-2.5 text-[13px] font-semibold text-white box-border'

export function Pagination({ currentPage, totalPages, onPageChange, useUrlParams }: PaginationProps) {
  const router = useRouter()
  const searchParams = useSearchParams()

  if (totalPages <= 1) return null

  const handlePageChange = (page: number) => {
    if (useUrlParams) {
      const params = new URLSearchParams(searchParams.toString())
      params.set('page', String(page))
      router.push(`?${params.toString()}`)
    }
    onPageChange?.(page)
  }

  const pages: (number | 'dots')[] = []
  if (totalPages <= 5) {
    for (let i = 0; i < totalPages; i++) pages.push(i)
  } else {
    pages.push(0)
    if (currentPage > 2) pages.push('dots')
    const start = Math.max(1, currentPage - 1)
    const end = Math.min(totalPages - 2, currentPage + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (currentPage < totalPages - 3) pages.push('dots')
    pages.push(totalPages - 1)
  }

  return (
    <div className="flex items-center gap-[5px]">
      <button
        type="button"
        onClick={() => handlePageChange(0)}
        disabled={currentPage === 0}
        className={PAGE_BTN}
        aria-label="First page"
      >
        <ChevronsLeft className="h-4 w-4" />
      </button>
      <button
        type="button"
        onClick={() => handlePageChange(currentPage - 1)}
        disabled={currentPage === 0}
        className={PAGE_BTN}
        aria-label="Previous page"
      >
        <ChevronLeft className="h-4 w-4" />
      </button>

      {pages.map((p, idx) =>
        p === 'dots' ? (
          <div
            key={`dots-${idx}`}
            className="flex h-8 w-8 items-center justify-center rounded-lg bg-white p-2.5 font-semibold"
          >
            ...
          </div>
        ) : (
          <button
            key={p}
            type="button"
            onClick={() => handlePageChange(p)}
            className={p === currentPage ? ACTIVE_BTN : PAGE_BTN}
          >
            {p + 1}
          </button>
        ),
      )}

      <button
        type="button"
        onClick={() => handlePageChange(currentPage + 1)}
        disabled={currentPage >= totalPages - 1}
        className={PAGE_BTN}
        aria-label="Next page"
      >
        <ChevronRight className="h-4 w-4" />
      </button>
      <button
        type="button"
        onClick={() => handlePageChange(totalPages - 1)}
        disabled={currentPage >= totalPages - 1}
        className={PAGE_BTN}
        aria-label="Last page"
      >
        <ChevronsRight className="h-4 w-4" />
      </button>
    </div>
  )
}
