'use client'

import { MoreHorizontal } from 'lucide-react'
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem } from '@/components/ui/dropdown-menu'
import { Skeleton } from '@/components/ui/skeleton'

export interface Column<T> {
  key: string
  header: string
  render: (item: T) => React.ReactNode
  hideOnMobile?: boolean
  className?: string
}

export interface RowAction<T> {
  label: string
  onClick: (item: T) => void
  variant?: 'default' | 'destructive'
}

interface DataTableProps<T> {
  columns: Column<T>[]
  data: T[]
  keyExtractor: (item: T) => string
  isLoading?: boolean
  skeletonCount?: number
  actions?: RowAction<T>[]
  onRowClick?: (item: T) => void
  emptyIcon?: React.ReactNode
  emptyTitle?: string
  emptyDescription?: string
}

export function DataTable<T>({
  columns,
  data,
  keyExtractor,
  isLoading,
  skeletonCount = 5,
  actions,
  onRowClick,
  emptyIcon,
  emptyTitle = 'No data found',
  emptyDescription,
}: DataTableProps<T>) {
  if (isLoading) {
    return (
      <div className="overflow-hidden rounded-2xl bg-white shadow-[0px_2px_6px_rgba(16,24,40,0.06)]">
        <div className="hidden md:block">
          <div className="border-b border-[#f0f0f0] px-6 py-3">
            <div className="flex gap-4">
              {columns.map((col) => (
                <Skeleton key={col.key} className="h-4 flex-1" />
              ))}
            </div>
          </div>
          {Array.from({ length: skeletonCount }).map((_, i) => (
            <div key={i} className="border-b border-[#f0f0f0] px-6 py-4">
              <div className="flex gap-4">
                {columns.map((col) => (
                  <Skeleton key={col.key} className="h-4 flex-1" />
                ))}
              </div>
            </div>
          ))}
        </div>
        <div className="flex flex-col gap-3 p-4 md:hidden">
          {Array.from({ length: skeletonCount }).map((_, i) => (
            <div key={i} className="rounded-xl border border-[#f0f0f0] p-4">
              <Skeleton className="mb-3 h-5 w-3/4" />
              <Skeleton className="mb-2 h-4 w-1/2" />
              <Skeleton className="h-4 w-1/3" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  if (data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 rounded-2xl bg-white py-20 shadow-[0px_2px_6px_rgba(16,24,40,0.06)]">
        {emptyIcon && <div className="text-[#a3a3a3]">{emptyIcon}</div>}
        <p className="font-heading text-lg font-medium text-[#1c3873]">{emptyTitle}</p>
        {emptyDescription && <p className="text-sm text-[#71717a]">{emptyDescription}</p>}
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-2xl bg-white shadow-[0px_2px_6px_rgba(16,24,40,0.06)]">
      {/* Desktop table */}
      <div className="hidden md:block overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#f0f0f0] text-left">
              {columns.map((col) => (
                <th
                  key={col.key}
                  scope="col"
                  className={`px-6 py-3.5 text-xs font-semibold uppercase tracking-wider text-[#71717a] ${col.className ?? ''}`}
                >
                  {col.header}
                </th>
              ))}
              {actions && actions.length > 0 && (
                <th scope="col" className="w-12 px-3 py-3.5">
                  <span className="sr-only">Actions</span>
                </th>
              )}
            </tr>
          </thead>
          <tbody>
            {data.map((item) => (
              <tr
                key={keyExtractor(item)}
                onClick={() => onRowClick?.(item)}
                className={`border-b border-[#f0f0f0] last:border-b-0 transition-colors hover:bg-[#f8faff] ${onRowClick ? 'cursor-pointer' : ''}`}
              >
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={`px-6 py-4 text-sm text-[#1b1c1a] ${col.className ?? ''}`}
                  >
                    {col.render(item)}
                  </td>
                ))}
                {actions && actions.length > 0 && (
                  <td className="px-3 py-4" onClick={(e) => e.stopPropagation()}>
                    <DropdownMenu>
                      <DropdownMenuTrigger
                        className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-[#f0f0f0]"
                        aria-label="Row actions"
                      >
                        <MoreHorizontal className="h-4 w-4 text-[#71717a]" aria-hidden="true" />
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        {actions.map((action) => (
                          <DropdownMenuItem
                            key={action.label}
                            onClick={(e) => {
                              e.stopPropagation()
                              action.onClick(item)
                            }}
                            className={action.variant === 'destructive' ? 'text-red-600 focus:text-red-600' : ''}
                          >
                            {action.label}
                          </DropdownMenuItem>
                        ))}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mobile cards */}
      <div className="flex flex-col gap-3 p-4 md:hidden">
        {data.map((item) => (
          <div
            key={keyExtractor(item)}
            onClick={() => onRowClick?.(item)}
            className={`rounded-xl border border-[#f0f0f0] p-4 transition-colors hover:bg-[#f8faff] ${onRowClick ? 'cursor-pointer' : ''}`}
          >
            <div className="flex items-start justify-between">
              <div className="flex flex-col gap-2">
                {columns
                  .filter((col) => !col.hideOnMobile)
                  .map((col) => (
                    <div key={col.key} className="flex items-center gap-2">
                      <span className="text-xs font-medium text-[#71717a]">{col.header}:</span>
                      <span className="text-sm text-[#1b1c1a]">{col.render(item)}</span>
                    </div>
                  ))}
              </div>
              {actions && actions.length > 0 && (
                <DropdownMenu>
                  <DropdownMenuTrigger
                    className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg transition-colors hover:bg-[#f0f0f0]"
                    onClick={(e) => e.stopPropagation()}
                    aria-label="Row actions"
                  >
                    <MoreHorizontal className="h-4 w-4 text-[#71717a]" aria-hidden="true" />
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    {actions.map((action) => (
                      <DropdownMenuItem
                        key={action.label}
                        onClick={(e) => {
                          e.stopPropagation()
                          action.onClick(item)
                        }}
                        className={action.variant === 'destructive' ? 'text-red-600 focus:text-red-600' : ''}
                      >
                        {action.label}
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
