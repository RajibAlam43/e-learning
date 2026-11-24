'use client'

import { useState, useEffect, useMemo } from 'react'
import { format } from 'date-fns'
import { toast } from 'sonner'
import { useAdminOrder, useUpdateOrder } from '@/hooks/useAdmin'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectTrigger,
  SelectContent,
  SelectItem,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'

interface OrderDetailDialogProps {
  orderId: string | null
  open: boolean
  onClose: () => void
}

const ORDER_STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'PAID', label: 'Paid' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'REFUNDED', label: 'Refunded' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

function formatDate(date: string | undefined): string {
  if (!date) return '—'
  try {
    return format(new Date(date), 'MMM d, yyyy')
  } catch {
    return '—'
  }
}

export function OrderDetailDialog({ orderId, open, onClose }: OrderDetailDialogProps) {
  const { data: order, isLoading } = useAdminOrder(orderId)
  const updateMutation = useUpdateOrder()

  const [adminNote, setAdminNote] = useState('')
  const [status, setStatus] = useState('')

  useEffect(() => {
    // Hydrate controls from the selected order before editing.
    if (order) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setAdminNote(order.adminNote ?? '')
      setStatus(order.status ?? 'PENDING')
    }
  }, [order])

  // Dirty detection — compare to original values.
  const isDirty = useMemo(() => {
    if (!order) return false
    if ((order.adminNote ?? '') !== adminNote) return true
    if ((order.status ?? 'PENDING') !== status) return true
    return false
  }, [order, adminNote, status])

  const isSaving = updateMutation.isPending

  // Block close during save; warn on close with unsaved changes.
  function handleOpenChange(next: boolean) {
    if (next) return
    if (isSaving) return
    if (isDirty) {
      const ok = window.confirm('You have unsaved changes. Discard them?')
      if (!ok) return
    }
    onClose()
  }

  function handleSave() {
    if (!orderId) return

    // Confirm destructive status changes.
    const criticalStatuses = ['REFUNDED', 'CANCELLED']
    if (criticalStatuses.includes(status) && status !== order?.status) {
      const ok = window.confirm(
        `Are you sure you want to mark this order as ${status}? This action cannot be undone.`,
      )
      if (!ok) return
    }

    updateMutation.mutate(
      { id: orderId, data: { status, adminNote: adminNote || undefined } },
      {
        onSuccess: () => {
          toast.success('Order updated successfully')
          onClose()
        },
        onError: (err) => {
          const e = err as { message?: string; status?: number }
          toast.error(e?.message || 'Failed to update order')
          if (e?.status === 422) {
            toast.warning('Some fields need attention. Check the form and try again.')
          }
        },
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="font-heading text-[#1c3873]">
            Order Details
          </DialogTitle>
        </DialogHeader>

        {isLoading ? (
          <div className="mt-4 space-y-3">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-20 w-full" />
          </div>
        ) : order ? (
          <div className="mt-4 space-y-5">
            {/* Customer info */}
            <div className="space-y-1.5 text-sm">
              <h3 className="font-heading font-semibold text-[#1c3873]">Customer</h3>
              <p className="text-[#5e5f5c]">
                <span className="font-medium text-[#1b1c1a]">{order.customerName}</span>
              </p>
              <p className="text-[#5e5f5c]">{order.customerEmail}</p>
              {order.customerPhone && (
                <p className="text-[#5e5f5c]">{order.customerPhone}</p>
              )}
            </div>

            {/* Order items */}
            <div className="space-y-2">
              <h3 className="text-sm font-heading font-semibold text-[#1c3873]">Items</h3>
              {order.items && order.items.length > 0 ? (
                <div className="space-y-2">
                  {order.items.map((item) => (
                    <div
                      key={item.courseId}
                      className="rounded-xl border border-[#f0f0f0] p-3 text-sm"
                    >
                      <p className="font-medium text-[#1b1c1a]">
                        {item.courseName}
                      </p>
                      <div className="mt-1 flex flex-wrap gap-x-4 gap-y-0.5 text-[#5e5f5c]">
                        <span>Price: ৳{(item.priceBdt ?? 0).toLocaleString()}</span>
                        {(item.discountBdt ?? 0) > 0 && (
                          <span>Discount: ৳{(item.discountBdt ?? 0).toLocaleString()}</span>
                        )}
                        <span className="font-medium text-[#1b1c1a]">
                          Final: ৳{(item.finalAmount ?? 0).toLocaleString()}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-[#71717a]">No items</p>
              )}
            </div>

            {/* Order meta */}
            <div className="grid grid-cols-2 gap-2 text-sm text-[#5e5f5c]">
              <div>
                <span className="text-xs uppercase text-[#71717a]">Total</span>
                <p className="font-medium text-[#1b1c1a]">
                  ৳{(order.totalAmount ?? 0).toLocaleString()}
                </p>
              </div>
              <div>
                <span className="text-xs uppercase text-[#71717a]">Provider</span>
                <p>{order.provider ?? '—'}</p>
              </div>
              <div>
                <span className="text-xs uppercase text-[#71717a]">Created</span>
                <p>{formatDate(order.createdAt)}</p>
              </div>
              <div>
                <span className="text-xs uppercase text-[#71717a]">Paid At</span>
                <p>{formatDate(order.paidAt)}</p>
              </div>
            </div>

            {/* Admin controls */}
            <div className="space-y-3 border-t border-[#f0f0f0] pt-4">
              <div className="space-y-1.5">
                <Label>Status</Label>
                <Select value={status} onValueChange={(v) => setStatus(v ?? 'PENDING')}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    {ORDER_STATUS_OPTIONS.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="admin-note">Admin Note</Label>
                <Textarea
                  id="admin-note"
                  value={adminNote}
                  onChange={(e) => setAdminNote(e.target.value)}
                  placeholder="Add an internal note..."
                  rows={3}
                />
              </div>
            </div>
          </div>
        ) : (
          <p className="mt-4 text-sm text-[#71717a]">Order not found.</p>
        )}

        <DialogFooter className="mt-4">
          <Button variant="outline" onClick={() => handleOpenChange(false)} disabled={isSaving}>
            Cancel
          </Button>
          <Button
            className="btn-blue-gradient text-white"
            onClick={handleSave}
            disabled={isSaving || isLoading || !isDirty}
          >
            {isSaving ? 'Saving…' : 'Save'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
