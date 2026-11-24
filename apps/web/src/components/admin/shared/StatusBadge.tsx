'use client'

import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

type StatusVariant = 'success' | 'warning' | 'error' | 'default' | 'info'

const STATUS_MAP: Record<string, { label: string; variant: StatusVariant }> = {
  PUBLISHED: { label: 'Published', variant: 'success' },
  DRAFT: { label: 'Draft', variant: 'warning' },
  ARCHIVED: { label: 'Archived', variant: 'default' },
  PAID: { label: 'Paid', variant: 'success' },
  PENDING: { label: 'Pending', variant: 'warning' },
  FAILED: { label: 'Failed', variant: 'error' },
  REFUNDED: { label: 'Refunded', variant: 'info' },
  CANCELLED: { label: 'Cancelled', variant: 'default' },
  SCHEDULED: { label: 'Scheduled', variant: 'info' },
  LIVE: { label: 'Live', variant: 'success' },
  COMPLETED: { label: 'Completed', variant: 'default' },
  ACTIVE: { label: 'Active', variant: 'success' },
  INACTIVE: { label: 'Inactive', variant: 'default' },
}

const VARIANT_STYLES: Record<StatusVariant, string> = {
  success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  warning: 'bg-amber-50 text-amber-700 border-amber-200',
  error: 'bg-red-50 text-red-700 border-red-200',
  info: 'bg-blue-50 text-blue-700 border-blue-200',
  default: 'bg-gray-50 text-gray-600 border-gray-200',
}

interface StatusBadgeProps {
  status: string
  className?: string
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const config = STATUS_MAP[status] ?? { label: status, variant: 'default' as StatusVariant }

  return (
    <Badge
      variant="outline"
      className={cn(
        'text-xs font-medium px-2.5 py-0.5 rounded-full border',
        VARIANT_STYLES[config.variant],
        className
      )}
    >
      {config.label}
    </Badge>
  )
}
